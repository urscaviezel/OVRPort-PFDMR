static PFN_xrGetSpaceBoundingBox2DFB real_box2;
static PFN_xrGetSpaceBoundingBox3DFB real_box3;
static XrSession scene_session;
static PFN_xrEnumerateSpaceSupportedComponentsFB real_components;
static XrResult scene_components(XrSpace space,uint32_t capacity,uint32_t *count,XrSpaceComponentTypeFB *out) {
    uint32_t needed=0;
    XrResult r=real_components(space,0,&needed,NULL);
    if(XR_FAILED(r)) return r;
    if(needed>64) return XR_ERROR_RUNTIME_FAILURE;
    XrSpaceComponentTypeFB components[64];
    if(needed) {
        r=real_components(space,needed,&needed,components);
        if(XR_FAILED(r)) return r;
        if(needed>64) return XR_ERROR_RUNTIME_FAILURE;
    }
    int scene=0;
    for(uint32_t i=0;i<needed;i++)
        if(components[i]==XR_SPACE_COMPONENT_TYPE_SEMANTIC_LABELS_FB ||
           components[i]==XR_SPACE_COMPONENT_TYPE_ROOM_LAYOUT_FB) scene=1;
    uint32_t kept=0;
    for(uint32_t i=0;i<needed;i++) {
        if(scene && components[i]==XR_SPACE_COMPONENT_TYPE_SHARABLE_FB) continue;
        if(scene && scene_session && components[i]==XR_SPACE_COMPONENT_TYPE_BOUNDED_3D_FB && real_box3) {
            XrRect3DfFB box={0};
            XrResult geometry=real_box3(scene_session,space,&box);
            if(geometry==XR_ERROR_SPACE_COMPONENT_NOT_ENABLED_FB) {
                LOG("scene omit absent volume space=%p",(void*)space);
                continue;
            }
        }
        if(scene && scene_session && components[i]==XR_SPACE_COMPONENT_TYPE_BOUNDED_2D_FB && real_box2) {
            XrRect2Df box={0};
            XrResult geometry=real_box2(scene_session,space,&box);
            if(geometry==XR_ERROR_SPACE_COMPONENT_NOT_ENABLED_FB) {
                LOG("scene omit absent plane space=%p",(void*)space);
                continue;
            }
        }
        components[kept++]=components[i];
    }
    *count=kept;
    if(scene) LOG("scene components space=%p advertised=%u local=%u capacity=%u",(void*)space,needed,kept,capacity);
    if(!capacity) return XR_SUCCESS;
    if(capacity<kept) return XR_ERROR_SIZE_INSUFFICIENT;
    if(kept && !out) return XR_ERROR_VALIDATION_FAILURE;
    if(kept) memcpy(out,components,kept*sizeof(*out));
    return XR_SUCCESS;
}
