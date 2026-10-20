#include "xr_dispatch.c"
#include <assert.h>
#include <stdio.h>
static int consumed;
static int event_index;
static XrAsyncRequestIdFB event_id;
static XrResult poll_mock(XrInstance instance,XrEventDataBuffer *event) {
    (void)instance;
    if(event_index>=3) return XR_EVENT_UNAVAILABLE;
    XrEventDataSpaceQueryResultsAvailableFB *e=(void*)event;
    e->type=(XrStructureType)(event_index++<2?1000247007:1000247008);
    e->requestId=event_id;
    return XR_SUCCESS;
}
static XrResult mock_discover(XrSession s,const DiscoveryInfo *i,XrAsyncRequestIdFB *id) {
    (void)s;(void)i;static unsigned seq=0;*id=++seq;return XR_SUCCESS;
}
static XrResult mock_retrieve(XrSession s,XrAsyncRequestIdFB id,DiscoveryResults *r) {
    (void)s;(void)id;r->resultCountOutput=consumed?0:3;
    if(consumed) return XR_SUCCESS;
    if(r->resultCapacityInput) {
        if(r->resultCapacityInput<3) return XR_ERROR_SIZE_INSUFFICIENT;
        for(int i=0;i<3;i++) {r->results[i].space=(XrSpace)(uintptr_t)(i+1);r->results[i].uuid.data[0]=i+1;}
        consumed=1;
    }
    return XR_SUCCESS;
}
static XrResult mock_room(XrSession s,XrSpace sp,XrRoomLayoutFB *r) {
    (void)s;r->wallUuidCountOutput=4;
    return sp==(XrSpace)(uintptr_t)2?XR_ERROR_RUNTIME_FAILURE:XR_SUCCESS;
}
int main(void) {
    real_discover=mock_discover;real_retrieve=mock_retrieve;dispatch_room=mock_room;
    ComponentFilter filter={.type=(XrStructureType)1000247004,.componentType=XR_SPACE_COMPONENT_TYPE_ROOM_LAYOUT_FB};
    const FilterHeader *filters[]={(const FilterHeader*)&filter};
    DiscoveryInfo info={.filterCount=1,.filters=filters};
    XrAsyncRequestIdFB id;
    assert(discover(0,&info,&id)==XR_SUCCESS);
    event_id=id;dispatch_poll=poll_mock;
    XrEventDataBuffer event={.type=XR_TYPE_EVENT_DATA_BUFFER};
    assert(poll_discovery(0,&event)==XR_SUCCESS && event.type==(XrStructureType)1000247007);
    assert(poll_discovery(0,&event)==XR_SUCCESS && event.type==(XrStructureType)1000247008);
    DiscoveryResult values[4]={0};
    DiscoveryResults out={0};
    assert(retrieve(0,id,&out)==XR_SUCCESS && out.resultCountOutput==2);
    assert(retrieve(0,id,&out)==XR_SUCCESS && out.resultCountOutput==2);
    out.resultCapacityInput=1;out.results=values;
    assert(retrieve(0,id,&out)==XR_ERROR_SIZE_INSUFFICIENT && out.resultCountOutput==2);
    out.resultCapacityInput=2;
    assert(retrieve(0,id,&out)==XR_SUCCESS && out.resultCountOutput==2);
    assert(values[0].uuid.data[0]==1 && values[1].uuid.data[0]==3 && values[2].space==0);
    out.resultCapacityInput=0;
    assert(retrieve(0,id,&out)==XR_SUCCESS && out.resultCountOutput==0);
    event_index=0;
    assert(poll_discovery(0,&event)==XR_SUCCESS && event.type==(XrStructureType)1000247008);
    assert(poll_discovery(0,&event)==XR_EVENT_UNAVAILABLE);
    consumed=0;
    info.filterCount=0;
    assert(discover(0,&info,&id)==XR_SUCCESS);
    out.resultCapacityInput=4;
    assert(retrieve(0,id,&out)==XR_SUCCESS && out.resultCountOutput==3);
    // Do not silently evict pending requests when the bounded table is full.
    memset(requests,0,sizeof(requests));
    for(int i=0;i<64;i++) assert(discover(0,&info,&id)==XR_SUCCESS);
    assert(discover(0,&info,&id)==XR_ERROR_LIMIT_REACHED);
    requests[10].complete=1;
    assert(discover(0,&info,&id)==XR_SUCCESS);
    puts("PASS: duplicate/empty notifications coalesced, completion preserved, consistent filtered count/data, repeated count, capacity retry, consuming runtime, unrelated searches unchanged");
}
