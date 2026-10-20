#include "xr_probe.c"
#include <assert.h>
static int starts,pauses,fail_start;
static XrPassthroughFlagsFB created_flags;
static XrResult mock_create(XrSession s,const XrPassthroughCreateInfoFB *i,XrPassthroughFB *o) {
    (void)s;created_flags=i->flags;*o=(XrPassthroughFB)(uintptr_t)0x100;return XR_SUCCESS;
}
static XrResult mock_destroy(XrPassthroughFB p) {(void)p;return XR_SUCCESS;}
static XrResult mock_start(XrPassthroughFB p) {(void)p;starts++;return fail_start?XR_ERROR_RUNTIME_FAILURE:XR_SUCCESS;}
static XrResult mock_pause(XrPassthroughFB p) {(void)p;pauses++;return XR_SUCCESS;}
static XrResult mock_layer(XrSession s,const XrPassthroughLayerCreateInfoFB *i,XrPassthroughLayerFB *o) {
    (void)s;(void)i;*o=(XrPassthroughLayerFB)(uintptr_t)0x200;return XR_SUCCESS;
}
static XrResult mock_destroy_layer(XrPassthroughLayerFB p) {(void)p;return XR_SUCCESS;}
static XrResult mock_end(XrSession s,const XrFrameEndInfo *f) {(void)s;(void)f;return XR_SUCCESS;}
static XrResult mock_end_session(XrSession s) {(void)s;return XR_SUCCESS;}
int main(void) {
    real_create_pt=mock_create;real_destroy_pt=mock_destroy;real_start_pt=mock_start;
    real_pause_pt=mock_pause;real_create_layer=mock_layer;real_destroy_layer=mock_destroy_layer;real_end=mock_end;
    XrPassthroughFB pt;
    XrPassthroughCreateInfoFB ci={.type=XR_TYPE_PASSTHROUGH_CREATE_INFO_FB,.flags=XR_PASSTHROUGH_IS_RUNNING_AT_CREATION_BIT_FB};
    assert(probe_create_pt(XR_NULL_HANDLE,&ci,&pt)==XR_SUCCESS && created_flags==0);
    XrPassthroughLayerFB layer;
    XrPassthroughLayerCreateInfoFB li={.type=XR_TYPE_PASSTHROUGH_LAYER_CREATE_INFO_FB,.passthrough=pt};
    assert(probe_create_layer(XR_NULL_HANDLE,&li,&layer)==XR_SUCCESS);
    XrCompositionLayerProjection projection={.type=XR_TYPE_COMPOSITION_LAYER_PROJECTION};
    XrCompositionLayerPassthroughFB passthrough={.type=XR_TYPE_COMPOSITION_LAYER_PASSTHROUGH_FB,.layerHandle=layer};
    const XrCompositionLayerBaseHeader *vr[]={ (const void*)&projection };
    const XrCompositionLayerBaseHeader *mr[]={ (const void*)&passthrough,(const void*)&projection };
    XrFrameEndInfo frame={.type=XR_TYPE_FRAME_END_INFO,.environmentBlendMode=XR_ENVIRONMENT_BLEND_MODE_OPAQUE,.layerCount=1,.layers=vr};
    probe_start_pt(pt);
    probe_end(XR_NULL_HANDLE,&frame);
    assert(starts==0 && pauses==0);
    frame.layerCount=2;frame.layers=mr;
    probe_end(XR_NULL_HANDLE,&frame);probe_end(XR_NULL_HANDLE,&frame);
    assert(starts==1 && pauses==0);
    frame.layerCount=1;frame.layers=vr;
    probe_end(XR_NULL_HANDLE,&frame);probe_end(XR_NULL_HANDLE,&frame);
    assert(pauses==1);
    probe_pause_pt(pt);
    frame.layerCount=2;frame.layers=mr;
    probe_end(XR_NULL_HANDLE,&frame);
    assert(starts==1); // Do not undo an application-requested pause.
    probe_start_pt(pt);fail_start=1;
    probe_end(XR_NULL_HANDLE,&frame);
    assert(starts==2 && pts[0].active==0);
    fail_start=0;probe_end(XR_NULL_HANDLE,&frame);
    assert(starts==3 && pts[0].active==1);
    real_end_session=mock_end_session;
    assert(probe_end_session(XR_NULL_HANDLE)==XR_SUCCESS);
    assert(pts[0].active==0 && pts[0].requested==1);
    probe_end(XR_NULL_HANDLE,&frame);
    assert(starts==4 && pts[0].active==1);
    probe_destroy_layer(layer);
    probe_end(XR_NULL_HANDLE,&frame);
    assert(pts[0].active==0);
    probe_destroy_pt(pt);
    assert(pts[0].handle==XR_NULL_HANDLE);
    puts("PASS: VR suppression, MR activation, transition deduplication, explicit pause, retry after failure, destroyed layer and object cleanup");
    return 0;
}
