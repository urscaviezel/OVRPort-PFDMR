#include "xr_probe.c"
#include <assert.h>
static int scene=1,fail=0;
static int geometry_test,has_volume;
static XrResult box2_mock(XrSession s,XrSpace space,XrRect2Df *out) {
    (void)s;(void)space;memset(out,0,sizeof(*out));return XR_SUCCESS;
}
static XrResult box3_mock(XrSession s,XrSpace space,XrRect3DfFB *out) {
    (void)s;(void)space;memset(out,0,sizeof(*out));
    return has_volume?XR_SUCCESS:XR_ERROR_SPACE_COMPONENT_NOT_ENABLED_FB;
}
static XrResult components_mock(XrSpace space,uint32_t cap,uint32_t *count,XrSpaceComponentTypeFB *out) {
    (void)space;
    if(fail) return XR_ERROR_RUNTIME_FAILURE;
    *count=geometry_test?6:(scene?4:3);
    if(!cap) return XR_SUCCESS;
    if(cap<*count) return XR_ERROR_SIZE_INSUFFICIENT;
    out[0]=XR_SPACE_COMPONENT_TYPE_LOCATABLE_FB;
    out[1]=XR_SPACE_COMPONENT_TYPE_SHARABLE_FB;
    out[2]=XR_SPACE_COMPONENT_TYPE_STORABLE_FB;
    if(scene) out[3]=XR_SPACE_COMPONENT_TYPE_SEMANTIC_LABELS_FB;
    if(geometry_test) {
        out[4]=XR_SPACE_COMPONENT_TYPE_BOUNDED_2D_FB;
        out[5]=XR_SPACE_COMPONENT_TYPE_BOUNDED_3D_FB;
    }
    return XR_SUCCESS;
}
int main(void) {
    real_components=components_mock;
    uint32_t count=0;
    XrSpaceComponentTypeFB values[4]={0,0,0,XR_SPACE_COMPONENT_TYPE_MAX_ENUM_FB};
    assert(scene_components(0,0,&count,NULL)==XR_SUCCESS && count==3);
    assert(scene_components(0,2,&count,values)==XR_ERROR_SIZE_INSUFFICIENT && count==3);
    assert(scene_components(0,3,&count,values)==XR_SUCCESS && count==3);
    assert(values[0]==XR_SPACE_COMPONENT_TYPE_LOCATABLE_FB && values[1]==XR_SPACE_COMPONENT_TYPE_STORABLE_FB);
    assert(values[2]==XR_SPACE_COMPONENT_TYPE_SEMANTIC_LABELS_FB && values[3]==XR_SPACE_COMPONENT_TYPE_MAX_ENUM_FB);
    scene=0;
    assert(scene_components(0,3,&count,values)==XR_SUCCESS && values[1]==XR_SPACE_COMPONENT_TYPE_SHARABLE_FB);
    fail=1;
    assert(scene_components(0,0,&count,NULL)==XR_ERROR_RUNTIME_FAILURE);
    fail=0;scene=1;geometry_test=1;scene_session=(XrSession)(uintptr_t)1;
    real_box2=box2_mock;real_box3=box3_mock;
    XrSpaceComponentTypeFB geom[6];
    assert(scene_components(0,0,&count,NULL)==XR_SUCCESS && count==4);
    assert(scene_components(0,6,&count,geom)==XR_SUCCESS && count==4);
    assert(geom[3]==XR_SPACE_COMPONENT_TYPE_BOUNDED_2D_FB);
    has_volume=1;
    assert(scene_components(0,6,&count,geom)==XR_SUCCESS && count==5);
    assert(geom[4]==XR_SPACE_COMPONENT_TYPE_BOUNDED_3D_FB);
    puts("PASS: scene-only sharing capability filtering, consistent count/fill, insufficient buffers, other anchor capabilities and failures preserved");
}
