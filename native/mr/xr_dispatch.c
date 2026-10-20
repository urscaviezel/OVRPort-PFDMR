// PFDMR optional MR discovery compatibility. GPL-3.0.
#include <openxr/openxr.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <string.h>
#include <stdlib.h>
/* ABI subset from Khronos OpenXR-SDK/include/openxr/openxr.h:
 * XR_META_spatial_entity_discovery (not in the bundled older SDK). */
typedef struct { XrStructureType type; const void *next; } FilterHeader;
typedef struct { XrStructureType type; const void *next; XrSpaceComponentTypeFB componentType; } ComponentFilter;
typedef struct { XrStructureType type; const void *next; uint32_t filterCount; const FilterHeader *const *filters; } DiscoveryInfo;
typedef struct { XrSpace space; XrUuidEXT uuid; } DiscoveryResult;
typedef struct { XrStructureType type; const void *next; uint32_t resultCapacityInput,resultCountOutput; DiscoveryResult *results; } DiscoveryResults;
typedef XrResult (*DiscoverFn)(XrSession,const DiscoveryInfo*,XrAsyncRequestIdFB*);
typedef XrResult (*RetrieveFn)(XrSession,XrAsyncRequestIdFB,DiscoveryResults*);
static DiscoverFn real_discover;
static RetrieveFn real_retrieve;
static PFN_xrGetSpaceRoomLayoutFB dispatch_room;
static PFN_xrPollEvent dispatch_poll;
static pthread_mutex_t discovery_lock=PTHREAD_MUTEX_INITIALIZER;
typedef struct { XrAsyncRequestIdFB id; XrSession session; int rooms,notified,complete; DiscoveryResult *pending; uint32_t count; } Request;
static Request requests[64];
#define DLOG(...) ((void)0)
static XrResult poll_discovery(XrInstance instance,XrEventDataBuffer *event) {
    for(;;) {
        XrResult r=dispatch_poll(instance,event);
        if(r==XR_SUCCESS && event->type==(XrStructureType)1000247008) {
            const XrEventDataSpaceQueryCompleteFB *complete=(const void*)event;
            pthread_mutex_lock(&discovery_lock);
            for(unsigned i=0;i<64;i++) if(requests[i].id==complete->requestId) requests[i].complete=1;
            pthread_mutex_unlock(&discovery_lock);
        }
        if(r!=XR_SUCCESS || event->type!=(XrStructureType)1000247007) return r;
        const XrEventDataSpaceQueryResultsAvailableFB *available=(const void*)event;
        int duplicate=0;
        pthread_mutex_lock(&discovery_lock);
        for(unsigned i=0;i<64;i++) if(requests[i].id==available->requestId) {
            duplicate=requests[i].notified;
            if(!duplicate && !requests[i].pending && real_retrieve) {
                DiscoveryResults count={.type=(XrStructureType)1000247006};
                XrResult query=real_retrieve(requests[i].session,available->requestId,&count);
                if(XR_SUCCEEDED(query) && !count.resultCountOutput) {
                    duplicate=1;
                    DLOG("discarded empty availability id=%llu",(unsigned long long)available->requestId);
                }
            }
            if(!duplicate) requests[i].notified=1;
            break;
        }
        pthread_mutex_unlock(&discovery_lock);
        if(!duplicate) return r;
        DLOG("coalesced duplicate availability id=%llu",(unsigned long long)available->requestId);
        /* The pending retrieval drains the available batch. Multiple queued
         * notifications for that same batch must not produce empty callbacks. */
        event->type=XR_TYPE_EVENT_DATA_BUFFER;
        event->next=NULL;
    }
}
static XrResult discover(XrSession session,const DiscoveryInfo *info,XrAsyncRequestIdFB *id) {
    int rooms=0;
    for(uint32_t i=0;i<info->filterCount;i++)
        if(info->filters[i]->type==(XrStructureType)1000247004 &&
           ((const ComponentFilter*)info->filters[i])->componentType==XR_SPACE_COMPONENT_TYPE_ROOM_LAYOUT_FB) rooms=1;
    pthread_mutex_lock(&discovery_lock);
    unsigned slot=64;
    for(unsigned i=0;i<64;i++) if(!requests[i].id || (requests[i].complete && !requests[i].pending && !requests[i].notified)) {slot=i;break;}
    if(slot==64) {pthread_mutex_unlock(&discovery_lock);return XR_ERROR_LIMIT_REACHED;}
    XrResult r=real_discover(session,info,id);
    if(XR_SUCCEEDED(r)) {
        free(requests[slot].pending);
        memset(&requests[slot],0,sizeof(requests[slot]));
        requests[slot].id=*id;requests[slot].rooms=rooms;requests[slot].session=session;
        DLOG("discover id=%llu rooms=%d",(unsigned long long)*id,rooms);
    }
    pthread_mutex_unlock(&discovery_lock);
    return r;
}
static XrResult retrieve(XrSession session,XrAsyncRequestIdFB id,DiscoveryResults *out) {
    pthread_mutex_lock(&discovery_lock);
    Request *request=NULL;
    for(unsigned i=0;i<64;i++) if(requests[i].id==id && requests[i].session==session && requests[i].rooms) request=&requests[i];
    if(!request || !dispatch_room || out->next) {
        pthread_mutex_unlock(&discovery_lock);
        XrResult r=real_retrieve(session,id,out);
        if(XR_SUCCEEDED(r) && out->resultCapacityInput) {
            pthread_mutex_lock(&discovery_lock);
            for(unsigned i=0;i<64;i++) if(requests[i].id==id) requests[i].notified=0;
            pthread_mutex_unlock(&discovery_lock);
        }
        return r;
    }
    XrResult r=XR_SUCCESS;
    if(!request->pending) {
        DiscoveryResults raw=*out;
        raw.resultCapacityInput=0;raw.resultCountOutput=0;raw.results=NULL;
        r=real_retrieve(session,id,&raw);
        if(XR_FAILED(r)) goto done;
        if(!raw.resultCountOutput) {out->resultCountOutput=0;goto done;}
        if(raw.resultCountOutput>4096) {r=XR_ERROR_RUNTIME_FAILURE;goto done;}
        raw.resultCapacityInput=raw.resultCountOutput;
        raw.results=calloc(raw.resultCapacityInput,sizeof(*raw.results));
        if(!raw.results) {r=XR_ERROR_OUT_OF_MEMORY;goto done;}
        r=real_retrieve(session,id,&raw);
        if(XR_FAILED(r)||raw.resultCountOutput>raw.resultCapacityInput) {
            free(raw.results);
            if(XR_SUCCEEDED(r)) r=XR_ERROR_RUNTIME_FAILURE;
            goto done;
        }
        uint32_t kept=0;
        for(uint32_t i=0;i<raw.resultCountOutput;i++) {
            XrRoomLayoutFB layout={.type=XR_TYPE_ROOM_LAYOUT_FB};
            XrResult rr=dispatch_room(session,raw.results[i].space,&layout);
            if(XR_FAILED(rr)) {
                DLOG("skip unreadable room space=%p result=%d",(void*)raw.results[i].space,rr);
                continue;
            }
            raw.results[kept++]=raw.results[i];
        }
        request->pending=raw.results;request->count=kept;
        DLOG("rooms returned=%u readable=%u (cached before count)",raw.resultCountOutput,kept);
    }
    out->resultCountOutput=request->count;
    if(out->resultCapacityInput) {
        if(out->resultCapacityInput<request->count) {r=XR_ERROR_SIZE_INSUFFICIENT;goto done;}
        if(request->count && !out->results) {r=XR_ERROR_VALIDATION_FAILURE;goto done;}
        if(request->count) memcpy(out->results,request->pending,request->count*sizeof(*out->results));
        free(request->pending);request->pending=NULL;request->count=0;
        request->notified=0;
    } else if(!request->count) {
        free(request->pending);request->pending=NULL;
    }
done:
    pthread_mutex_unlock(&discovery_lock);
    return r;
}
static pthread_once_t dispatch_once=PTHREAD_ONCE_INIT;
static void *overport;
static PFN_xrGetInstanceProcAddr original_gipa;
static void initialize_dispatch(void) {
    overport=dlopen("libpfdmr_mr_original_openxr.so",RTLD_NOW|RTLD_LOCAL);
    if(overport) original_gipa=(PFN_xrGetInstanceProcAddr)dlsym(overport,"xrGetInstanceProcAddr");

}
XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProcAddr(XrInstance instance,const char *name,PFN_xrVoidFunction *fn) {
    pthread_once(&dispatch_once,initialize_dispatch);
    if(!original_gipa) return XR_ERROR_INITIALIZATION_FAILED;
    XrResult result=original_gipa(instance,name,fn);
    if(XR_SUCCEEDED(result) && *fn) {
        if(!strcmp(name,"xrGetInstanceProcAddr")) { *fn=(PFN_xrVoidFunction)xrGetInstanceProcAddr; return XR_SUCCESS; }
        if(!strcmp(name,"xrGetSpaceRoomLayoutFB")) dispatch_room=(PFN_xrGetSpaceRoomLayoutFB)*fn;
        if(!strcmp(name,"xrPollEvent")) {dispatch_poll=(PFN_xrPollEvent)*fn;*fn=(PFN_xrVoidFunction)poll_discovery;}
        if(!strcmp(name,"xrDiscoverSpacesMETA")) {real_discover=(DiscoverFn)*fn;*fn=(PFN_xrVoidFunction)discover;}
        if(!strcmp(name,"xrRetrieveSpaceDiscoveryResultsMETA")) {real_retrieve=(RetrieveFn)*fn;*fn=(PFN_xrVoidFunction)retrieve;}
    }
    return result;
}
XRAPI_ATTR XrResult XRAPI_CALL xrSuggestInteractionProfileBindings(XrInstance instance,const XrInteractionProfileSuggestedBinding *bindings) {
    pthread_once(&dispatch_once,initialize_dispatch);
    PFN_xrSuggestInteractionProfileBindings f=overport?(PFN_xrSuggestInteractionProfileBindings)dlsym(overport,"xrSuggestInteractionProfileBindings"):0;
    return f?f(instance,bindings):XR_ERROR_FUNCTION_UNSUPPORTED;
}

void* mr_sym(const char* name) { pthread_once(&dispatch_once,initialize_dispatch);return overport?dlsym(overport,name):NULL; }
