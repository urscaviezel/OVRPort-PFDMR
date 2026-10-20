#include "xr_probe.c"
#include <assert.h>
static int terminated,fail;
static XrResult labels_mock(XrSession s,XrSpace sp,XrSemanticLabelsFB *p) {
    (void)s;(void)sp;
    if(fail) return XR_ERROR_RUNTIME_FAILURE;
    p->bufferCountOutput=9+terminated;
    if(!p->bufferCapacityInput) return XR_SUCCESS;
    if(p->bufferCapacityInput!=9u+terminated) return XR_ERROR_RUNTIME_FAILURE;
    memcpy(p->buffer,"WALL_FACE",9+terminated);
    return XR_SUCCESS;
}
static XrResult room_mock(XrSession s,XrSpace sp,XrRoomLayoutFB *p) {
    (void)s;(void)sp;
    if(fail) return XR_ERROR_RUNTIME_FAILURE;
    p->wallUuidCountOutput=4;
    p->floorUuid.data[0]=42;
    if(!p->wallUuidCapacityInput) return XR_SUCCESS;
    if(p->wallUuidCapacityInput!=4) return XR_ERROR_RUNTIME_FAILURE;
    memset(p->wallUuids,7,4*sizeof(XrUuidEXT));
    return XR_SUCCESS;
}
int main(void) {
    real_labels=labels_mock;real_room=room_mock;
    char buffer[1024];
    XrSemanticLabelsFB l={.type=XR_TYPE_SEMANTIC_LABELS_FB,.bufferCapacityInput=sizeof(buffer),.buffer=buffer};
    for(terminated=0;terminated<=1;terminated++) {
        memset(buffer,99,sizeof(buffer));
        assert(probe_labels(0,0,&l)==XR_SUCCESS);
        assert(l.bufferCountOutput==9u+terminated);
        assert(memcmp(buffer,"WALL_FACE",9+terminated)==0 && buffer[9+terminated]==99);
    }
    terminated=0;
    l.bufferCapacityInput=9;
    assert(probe_labels(0,0,&l)==XR_SUCCESS && l.bufferCountOutput==9);
    l.bufferCapacityInput=8;
    buffer[0]=99;
    assert(probe_labels(0,0,&l)==XR_ERROR_SIZE_INSUFFICIENT && buffer[0]==99);
    l.bufferCapacityInput=0;l.buffer=NULL;
    assert(probe_labels(0,0,&l)==XR_SUCCESS && l.bufferCountOutput==9);
    XrUuidEXT walls[16]={0};
    XrRoomLayoutFB room={.type=XR_TYPE_ROOM_LAYOUT_FB,.wallUuidCapacityInput=16,.wallUuids=walls};
    assert(probe_room(0,0,&room)==XR_SUCCESS && room.wallUuidCountOutput==4);
    assert(room.floorUuid.data[0]==42 && walls[3].data[0]==7 && walls[4].data[0]==0);
    room.wallUuidCapacityInput=3;
    assert(probe_room(0,0,&room)==XR_ERROR_SIZE_INSUFFICIENT);
    room.wallUuidCapacityInput=0;room.wallUuids=NULL;
    assert(probe_room(0,0,&room)==XR_SUCCESS && room.wallUuidCountOutput==4);
    fail=1;
    assert(probe_labels(0,0,&l)==XR_ERROR_RUNTIME_FAILURE);
    assert(probe_room(0,0,&room)==XR_ERROR_RUNTIME_FAILURE);
    puts("PASS: exact runtime capacities, terminated/unterminated labels, oversized/undersized/query buffers, UUID bounds, error propagation");
    return 0;
}
