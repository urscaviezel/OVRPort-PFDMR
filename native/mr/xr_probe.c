// PFDMR optional MR compatibility. GPL-3.0. No Unreal runtime hooks.
#include <openxr/openxr.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define LOG(...) ((void)0)
#include "scene_components.h"
static void *runtime;
static PFN_xrGetInstanceProcAddr real_gipa;
static pthread_once_t once = PTHREAD_ONCE_INIT;
static PFN_xrEndFrame real_end;
static PFN_xrEndSession real_end_session;
static PFN_xrCreatePassthroughFB real_create_pt;
static PFN_xrDestroyPassthroughFB real_destroy_pt;
static PFN_xrPassthroughStartFB real_start_pt;
static PFN_xrPassthroughPauseFB real_pause_pt;
static PFN_xrCreatePassthroughLayerFB real_create_layer;
static PFN_xrDestroyPassthroughLayerFB real_destroy_layer;
static PFN_xrGetSpaceSemanticLabelsFB real_labels;
static PFN_xrGetSpaceRoomLayoutFB real_room;
/* This runtime rejects oversized buffers. Query first and pass the exact
 * requested capacity, while retaining the application's original buffers. */
static XrResult probe_labels(XrSession session,XrSpace space,XrSemanticLabelsFB *out) {
    XrSemanticLabelsFB query=*out;
    query.bufferCapacityInput=0;
    query.bufferCountOutput=0;
    query.buffer=NULL;
    XrResult r=real_labels(session,space,&query);
    LOG("labels query space=%p capacity=%u needed=%u result=%d",
        (void*)space,out->bufferCapacityInput,query.bufferCountOutput,r);
    if(XR_FAILED(r)) return r;
    uint32_t needed=query.bufferCountOutput;
    if(needed>65536) return XR_ERROR_RUNTIME_FAILURE;
    char *buffer=calloc((size_t)needed+1,1);
    if(!buffer) return XR_ERROR_OUT_OF_MEMORY;
    if(needed) {
        query.bufferCapacityInput=needed;
        query.buffer=buffer;
        r=real_labels(session,space,&query);
    }
    if(XR_SUCCEEDED(r)) {
        /* Preserve the runtime's byte count. OVRPlugin converts exactly this
         * many bytes to FString and supplies its own terminator. Adding one
         * here makes the terminator part of the semantic label's contents. */
        uint32_t count=query.bufferCountOutput;
        if(count>needed) { free(buffer); return XR_ERROR_RUNTIME_FAILURE; }
        out->bufferCountOutput=count;
        if(out->bufferCapacityInput) {
            if(out->bufferCapacityInput<count) r=XR_ERROR_SIZE_INSUFFICIENT;
            else if(!out->buffer) r=XR_ERROR_VALIDATION_FAILURE;
            else memcpy(out->buffer,buffer,count);
        }
        LOG("labels read space=%p label=%s count=%u result=%d",(void*)space,buffer,count,r);
    } else LOG("labels read failed space=%p result=%d",(void*)space,r);
    free(buffer);
    return r;
}
static XrResult probe_room(XrSession session,XrSpace space,XrRoomLayoutFB *out) {
    XrRoomLayoutFB query=*out;
    query.wallUuidCapacityInput=0;
    query.wallUuidCountOutput=0;
    query.wallUuids=NULL;
    XrResult r=real_room(session,space,&query);
    LOG("room query space=%p capacity=%u needed=%u result=%d",
        (void*)space,out->wallUuidCapacityInput,query.wallUuidCountOutput,r);
    if(XR_FAILED(r)) return r;
    out->wallUuidCountOutput=query.wallUuidCountOutput;
    out->floorUuid=query.floorUuid;
    out->ceilingUuid=query.ceilingUuid;
    if(!out->wallUuidCapacityInput) return r;
    if(out->wallUuidCapacityInput<query.wallUuidCountOutput) return XR_ERROR_SIZE_INSUFFICIENT;
    if(query.wallUuidCountOutput && !out->wallUuids) return XR_ERROR_VALIDATION_FAILURE;
    query.wallUuidCapacityInput=query.wallUuidCountOutput;
    query.wallUuids=out->wallUuids;
    r=real_room(session,space,&query);
    out->wallUuidCountOutput=query.wallUuidCountOutput;
    if(XR_SUCCEEDED(r)) {
        out->floorUuid=query.floorUuid;
        out->ceilingUuid=query.ceilingUuid;
    }
    LOG("room read space=%p count=%u result=%d",(void*)space,query.wallUuidCountOutput,r);
    return r;
}
static struct { XrPassthroughFB handle; int requested, active; } pts[16];
static struct { XrPassthroughLayerFB handle; XrPassthroughFB parent; } layers[64];
static pthread_mutex_t pt_lock=PTHREAD_MUTEX_INITIALIZER;
static int gate=1;
static XrResult probe_end_session(XrSession session) {
    pthread_mutex_lock(&pt_lock);
    for(int i=0;i<16;i++) if(gate && pts[i].handle && pts[i].active && real_pause_pt) {
        XrResult r=real_pause_pt(pts[i].handle);
        LOG("session handoff pause=%p result=%d",(void*)pts[i].handle,r);
        if(XR_SUCCEEDED(r)) pts[i].active=0;
    }
    pthread_mutex_unlock(&pt_lock);
    return real_end_session(session);
}
static XrResult probe_create_pt(XrSession session,const XrPassthroughCreateInfoFB *info,XrPassthroughFB *out) {
    scene_session=session;
    XrPassthroughCreateInfoFB copy=*info;
    if(gate) copy.flags &= ~XR_PASSTHROUGH_IS_RUNNING_AT_CREATION_BIT_FB;
    XrResult r=real_create_pt(session,&copy,out);
    if(XR_SUCCEEDED(r)) {
        pthread_mutex_lock(&pt_lock);
        for(int i=0;i<16;i++) if(!pts[i].handle) {
            pts[i].handle=*out;
            pts[i].requested=!!(info->flags & XR_PASSTHROUGH_IS_RUNNING_AT_CREATION_BIT_FB);
            pts[i].active=gate?0:pts[i].requested;
            break;
        }
        pthread_mutex_unlock(&pt_lock);
        LOG("create passthrough=%p gate=%d result=%d",(void*)*out,gate,r);
    }
    return r;
}
static XrResult probe_start_pt(XrPassthroughFB pt) {
    pthread_mutex_lock(&pt_lock);
    for(int i=0;i<16;i++) if(pts[i].handle==pt) {
        pts[i].requested=1;
        XrResult r=gate?XR_SUCCESS:real_start_pt(pt);
        if(!gate && XR_SUCCEEDED(r)) pts[i].active=1;
        pthread_mutex_unlock(&pt_lock);
        return r;
    }
    pthread_mutex_unlock(&pt_lock);
    return real_start_pt(pt);
}
static XrResult probe_pause_pt(XrPassthroughFB pt) {
    pthread_mutex_lock(&pt_lock);
    for(int i=0;i<16;i++) if(gate && pts[i].handle==pt && !pts[i].active) {
        pts[i].requested=0;
        pthread_mutex_unlock(&pt_lock);
        return XR_SUCCESS;
    }
    XrResult r=real_pause_pt(pt);
    if(XR_SUCCEEDED(r)) for(int i=0;i<16;i++) if(pts[i].handle==pt) pts[i].requested=pts[i].active=0;
    pthread_mutex_unlock(&pt_lock);
    return r;
}
static XrResult probe_destroy_pt(XrPassthroughFB pt) {
    pthread_mutex_lock(&pt_lock);
    XrResult r=real_destroy_pt(pt);
    if(XR_SUCCEEDED(r)) for(int i=0;i<16;i++) if(pts[i].handle==pt) memset(&pts[i],0,sizeof(pts[i]));
    pthread_mutex_unlock(&pt_lock);
    return r;
}
static XrResult probe_create_layer(XrSession session,const XrPassthroughLayerCreateInfoFB *info,XrPassthroughLayerFB *out) {
    XrResult r=real_create_layer(session,info,out);
    if(XR_SUCCEEDED(r)) {
        pthread_mutex_lock(&pt_lock);
        for(int i=0;i<64;i++) if(!layers[i].handle) {layers[i].handle=*out;layers[i].parent=info->passthrough;break;}
        pthread_mutex_unlock(&pt_lock);
    }
    return r;
}
static XrResult probe_destroy_layer(XrPassthroughLayerFB layer) {
    pthread_mutex_lock(&pt_lock);
    XrResult r=real_destroy_layer(layer);
    if(XR_SUCCEEDED(r)) for(int i=0;i<64;i++) if(layers[i].handle==layer) memset(&layers[i],0,sizeof(layers[i]));
    pthread_mutex_unlock(&pt_lock);
    return r;
}
static void init(void) {
    runtime = dlopen("libpfdmr_mr_original_yvr.so", RTLD_NOW|RTLD_LOCAL);
    if (runtime) real_gipa=(PFN_xrGetInstanceProcAddr)dlsym(runtime,"xrGetInstanceProcAddr");
    LOG("fix v15 label length loaded gate=%d runtime=%p gipa=%p error=%s",gate,runtime,real_gipa,runtime?"none":dlerror());
}
static XrResult probe_end(XrSession session,const XrFrameEndInfo *f) {
    if(gate && real_start_pt && real_pause_pt) {
        pthread_mutex_lock(&pt_lock);
        for(int p=0;p<16;p++) if(pts[p].handle) {
            int submitted=0;
            for(uint32_t i=0;i<f->layerCount;i++) if(f->layers[i]->type==XR_TYPE_COMPOSITION_LAYER_PASSTHROUGH_FB) {
                XrPassthroughLayerFB h=((const XrCompositionLayerPassthroughFB*)f->layers[i])->layerHandle;
                for(int j=0;j<64;j++) if(layers[j].handle==h && layers[j].parent==pts[p].handle) submitted=1;
            }
            int wanted=submitted && pts[p].requested;
            if(wanted!=pts[p].active) {
                XrResult r=wanted?real_start_pt(pts[p].handle):real_pause_pt(pts[p].handle);
                LOG("gate passthrough=%p submitted=%d requested=%d active=%d -> %d result=%d",
                    (void*)pts[p].handle,submitted,pts[p].requested,pts[p].active,wanted,r);
                if(XR_SUCCEEDED(r)) pts[p].active=wanted;
            }
        }
        pthread_mutex_unlock(&pt_lock);
    }
    return real_end(session,f);
}
XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProcAddr(XrInstance instance,const char *name,PFN_xrVoidFunction *fn) {
    pthread_once(&once,init);
    if(!real_gipa) return XR_ERROR_INITIALIZATION_FAILED;
    XrResult r=real_gipa(instance,name,fn);
    if(XR_SUCCEEDED(r) && *fn) {
        if(!strcmp(name,"xrGetSpaceBoundingBox2DFB")) real_box2=(PFN_xrGetSpaceBoundingBox2DFB)*fn;
        if(!strcmp(name,"xrGetSpaceBoundingBox3DFB")) real_box3=(PFN_xrGetSpaceBoundingBox3DFB)*fn;
        if(!strcmp(name,"xrEndFrame")) {real_end=(PFN_xrEndFrame)*fn; *fn=(PFN_xrVoidFunction)probe_end;}
#define HOOK(api,slot,hook) if(!strcmp(name,#api)) {slot=(PFN_##api)*fn;*fn=(PFN_xrVoidFunction)hook;}
        HOOK(xrEndSession,real_end_session,probe_end_session)
        HOOK(xrEnumerateSpaceSupportedComponentsFB,real_components,scene_components)
        HOOK(xrCreatePassthroughFB,real_create_pt,probe_create_pt)
        HOOK(xrDestroyPassthroughFB,real_destroy_pt,probe_destroy_pt)
        HOOK(xrPassthroughStartFB,real_start_pt,probe_start_pt)
        HOOK(xrPassthroughPauseFB,real_pause_pt,probe_pause_pt)
        HOOK(xrCreatePassthroughLayerFB,real_create_layer,probe_create_layer)
        HOOK(xrDestroyPassthroughLayerFB,real_destroy_layer,probe_destroy_layer)
        HOOK(xrGetSpaceSemanticLabelsFB,real_labels,probe_labels)
        HOOK(xrGetSpaceRoomLayoutFB,real_room,probe_room)
#undef HOOK
    }
    return r;
}
XRAPI_ATTR XrResult XRAPI_CALL xrInitializeLoaderKHR(const XrLoaderInitInfoBaseHeaderKHR *info) {
    pthread_once(&once,init);
    PFN_xrInitializeLoaderKHR f=NULL;
    if(real_gipa) real_gipa(XR_NULL_HANDLE,"xrInitializeLoaderKHR",(PFN_xrVoidFunction*)&f);
    if(!f && runtime) f=(PFN_xrInitializeLoaderKHR)dlsym(runtime,"xrInitializeLoaderKHR");
    return f?f(info):XR_ERROR_FUNCTION_UNSUPPORTED;
}

void* mr_yvr_sym(const char* name) {pthread_once(&once,init);return runtime?dlsym(runtime,name):NULL;}
XRAPI_ATTR XrResult XRAPI_CALL xrEndFrame(XrSession session,const XrFrameEndInfo* info) {
 pthread_once(&once,init);if(!real_end) real_end=(PFN_xrEndFrame)mr_yvr_sym("xrEndFrame");
 return real_end?probe_end(session,info):XR_ERROR_FUNCTION_UNSUPPORTED;
}
XRAPI_ATTR XrResult XRAPI_CALL xrEndSession(XrSession session) {
 pthread_once(&once,init);if(!real_end_session) real_end_session=(PFN_xrEndSession)mr_yvr_sym("xrEndSession");
 return real_end_session?probe_end_session(session):XR_ERROR_FUNCTION_UNSUPPORTED;
}
