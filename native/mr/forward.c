#include <openxr/openxr.h>
extern void* mr_sym(const char*);
XRAPI_ATTR XrResult XRAPI_CALL xrAcquireSwapchainImage(
    XrSwapchain                                 swapchain,
    const XrSwapchainImageAcquireInfo*          acquireInfo,
    uint32_t*                                   index) { PFN_xrAcquireSwapchainImage fn=(PFN_xrAcquireSwapchainImage)mr_sym("xrAcquireSwapchainImage"); return fn ? fn(swapchain, acquireInfo, index) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrApplyHapticFeedback(
    XrSession                                   session,
    const XrHapticActionInfo*                   hapticActionInfo,
    const XrHapticBaseHeader*                   hapticFeedback) { PFN_xrApplyHapticFeedback fn=(PFN_xrApplyHapticFeedback)mr_sym("xrApplyHapticFeedback"); return fn ? fn(session, hapticActionInfo, hapticFeedback) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrAttachSessionActionSets(
    XrSession                                   session,
    const XrSessionActionSetsAttachInfo*        attachInfo) { PFN_xrAttachSessionActionSets fn=(PFN_xrAttachSessionActionSets)mr_sym("xrAttachSessionActionSets"); return fn ? fn(session, attachInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrBeginFrame(
    XrSession                                   session,
    const XrFrameBeginInfo*                     frameBeginInfo) { PFN_xrBeginFrame fn=(PFN_xrBeginFrame)mr_sym("xrBeginFrame"); return fn ? fn(session, frameBeginInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrBeginSession(
    XrSession                                   session,
    const XrSessionBeginInfo*                   beginInfo) { PFN_xrBeginSession fn=(PFN_xrBeginSession)mr_sym("xrBeginSession"); return fn ? fn(session, beginInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrCreateAction(
    XrActionSet                                 actionSet,
    const XrActionCreateInfo*                   createInfo,
    XrAction*                                   action) { PFN_xrCreateAction fn=(PFN_xrCreateAction)mr_sym("xrCreateAction"); return fn ? fn(actionSet, createInfo, action) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrCreateActionSet(
    XrInstance                                  instance,
    const XrActionSetCreateInfo*                createInfo,
    XrActionSet*                                actionSet) { PFN_xrCreateActionSet fn=(PFN_xrCreateActionSet)mr_sym("xrCreateActionSet"); return fn ? fn(instance, createInfo, actionSet) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrCreateActionSpace(
    XrSession                                   session,
    const XrActionSpaceCreateInfo*              createInfo,
    XrSpace*                                    space) { PFN_xrCreateActionSpace fn=(PFN_xrCreateActionSpace)mr_sym("xrCreateActionSpace"); return fn ? fn(session, createInfo, space) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrCreateInstance(
    const XrInstanceCreateInfo*                 createInfo,
    XrInstance*                                 instance) { PFN_xrCreateInstance fn=(PFN_xrCreateInstance)mr_sym("xrCreateInstance"); return fn ? fn(createInfo, instance) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrCreateReferenceSpace(
    XrSession                                   session,
    const XrReferenceSpaceCreateInfo*           createInfo,
    XrSpace*                                    space) { PFN_xrCreateReferenceSpace fn=(PFN_xrCreateReferenceSpace)mr_sym("xrCreateReferenceSpace"); return fn ? fn(session, createInfo, space) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrCreateSession(
    XrInstance                                  instance,
    const XrSessionCreateInfo*                  createInfo,
    XrSession*                                  session) { PFN_xrCreateSession fn=(PFN_xrCreateSession)mr_sym("xrCreateSession"); return fn ? fn(instance, createInfo, session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrCreateSwapchain(
    XrSession                                   session,
    const XrSwapchainCreateInfo*                createInfo,
    XrSwapchain*                                swapchain) { PFN_xrCreateSwapchain fn=(PFN_xrCreateSwapchain)mr_sym("xrCreateSwapchain"); return fn ? fn(session, createInfo, swapchain) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrDestroyAction(
    XrAction                                    action) { PFN_xrDestroyAction fn=(PFN_xrDestroyAction)mr_sym("xrDestroyAction"); return fn ? fn(action) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrDestroyActionSet(
    XrActionSet                                 actionSet) { PFN_xrDestroyActionSet fn=(PFN_xrDestroyActionSet)mr_sym("xrDestroyActionSet"); return fn ? fn(actionSet) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrDestroyInstance(
    XrInstance                                  instance) { PFN_xrDestroyInstance fn=(PFN_xrDestroyInstance)mr_sym("xrDestroyInstance"); return fn ? fn(instance) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrDestroySession(
    XrSession                                   session) { PFN_xrDestroySession fn=(PFN_xrDestroySession)mr_sym("xrDestroySession"); return fn ? fn(session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrDestroySpace(
    XrSpace                                     space) { PFN_xrDestroySpace fn=(PFN_xrDestroySpace)mr_sym("xrDestroySpace"); return fn ? fn(space) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrDestroySwapchain(
    XrSwapchain                                 swapchain) { PFN_xrDestroySwapchain fn=(PFN_xrDestroySwapchain)mr_sym("xrDestroySwapchain"); return fn ? fn(swapchain) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEndFrame(
    XrSession                                   session,
    const XrFrameEndInfo*                       frameEndInfo) { PFN_xrEndFrame fn=(PFN_xrEndFrame)mr_sym("xrEndFrame"); return fn ? fn(session, frameEndInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEndSession(
    XrSession                                   session) { PFN_xrEndSession fn=(PFN_xrEndSession)mr_sym("xrEndSession"); return fn ? fn(session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateApiLayerProperties(
    uint32_t                                    propertyCapacityInput,
    uint32_t*                                   propertyCountOutput,
    XrApiLayerProperties*                       properties) { PFN_xrEnumerateApiLayerProperties fn=(PFN_xrEnumerateApiLayerProperties)mr_sym("xrEnumerateApiLayerProperties"); return fn ? fn(propertyCapacityInput, propertyCountOutput, properties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateBoundSourcesForAction(
    XrSession                                   session,
    const XrBoundSourcesForActionEnumerateInfo* enumerateInfo,
    uint32_t                                    sourceCapacityInput,
    uint32_t*                                   sourceCountOutput,
    XrPath*                                     sources) { PFN_xrEnumerateBoundSourcesForAction fn=(PFN_xrEnumerateBoundSourcesForAction)mr_sym("xrEnumerateBoundSourcesForAction"); return fn ? fn(session, enumerateInfo, sourceCapacityInput, sourceCountOutput, sources) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateEnvironmentBlendModes(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrViewConfigurationType                     viewConfigurationType,
    uint32_t                                    environmentBlendModeCapacityInput,
    uint32_t*                                   environmentBlendModeCountOutput,
    XrEnvironmentBlendMode*                     environmentBlendModes) { PFN_xrEnumerateEnvironmentBlendModes fn=(PFN_xrEnumerateEnvironmentBlendModes)mr_sym("xrEnumerateEnvironmentBlendModes"); return fn ? fn(instance, systemId, viewConfigurationType, environmentBlendModeCapacityInput, environmentBlendModeCountOutput, environmentBlendModes) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateInstanceExtensionProperties(
    const char*                                 layerName,
    uint32_t                                    propertyCapacityInput,
    uint32_t*                                   propertyCountOutput,
    XrExtensionProperties*                      properties) { PFN_xrEnumerateInstanceExtensionProperties fn=(PFN_xrEnumerateInstanceExtensionProperties)mr_sym("xrEnumerateInstanceExtensionProperties"); return fn ? fn(layerName, propertyCapacityInput, propertyCountOutput, properties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateReferenceSpaces(
    XrSession                                   session,
    uint32_t                                    spaceCapacityInput,
    uint32_t*                                   spaceCountOutput,
    XrReferenceSpaceType*                       spaces) { PFN_xrEnumerateReferenceSpaces fn=(PFN_xrEnumerateReferenceSpaces)mr_sym("xrEnumerateReferenceSpaces"); return fn ? fn(session, spaceCapacityInput, spaceCountOutput, spaces) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateSwapchainFormats(
    XrSession                                   session,
    uint32_t                                    formatCapacityInput,
    uint32_t*                                   formatCountOutput,
    int64_t*                                    formats) { PFN_xrEnumerateSwapchainFormats fn=(PFN_xrEnumerateSwapchainFormats)mr_sym("xrEnumerateSwapchainFormats"); return fn ? fn(session, formatCapacityInput, formatCountOutput, formats) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateSwapchainImages(
    XrSwapchain                                 swapchain,
    uint32_t                                    imageCapacityInput,
    uint32_t*                                   imageCountOutput,
    XrSwapchainImageBaseHeader*                 images) { PFN_xrEnumerateSwapchainImages fn=(PFN_xrEnumerateSwapchainImages)mr_sym("xrEnumerateSwapchainImages"); return fn ? fn(swapchain, imageCapacityInput, imageCountOutput, images) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateViewConfigurationViews(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrViewConfigurationType                     viewConfigurationType,
    uint32_t                                    viewCapacityInput,
    uint32_t*                                   viewCountOutput,
    XrViewConfigurationView*                    views) { PFN_xrEnumerateViewConfigurationViews fn=(PFN_xrEnumerateViewConfigurationViews)mr_sym("xrEnumerateViewConfigurationViews"); return fn ? fn(instance, systemId, viewConfigurationType, viewCapacityInput, viewCountOutput, views) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateViewConfigurations(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    uint32_t                                    viewConfigurationTypeCapacityInput,
    uint32_t*                                   viewConfigurationTypeCountOutput,
    XrViewConfigurationType*                    viewConfigurationTypes) { PFN_xrEnumerateViewConfigurations fn=(PFN_xrEnumerateViewConfigurations)mr_sym("xrEnumerateViewConfigurations"); return fn ? fn(instance, systemId, viewConfigurationTypeCapacityInput, viewConfigurationTypeCountOutput, viewConfigurationTypes) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStateBoolean(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStateBoolean*                       state) { PFN_xrGetActionStateBoolean fn=(PFN_xrGetActionStateBoolean)mr_sym("xrGetActionStateBoolean"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStateFloat(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStateFloat*                         state) { PFN_xrGetActionStateFloat fn=(PFN_xrGetActionStateFloat)mr_sym("xrGetActionStateFloat"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStatePose(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStatePose*                          state) { PFN_xrGetActionStatePose fn=(PFN_xrGetActionStatePose)mr_sym("xrGetActionStatePose"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStateVector2f(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStateVector2f*                      state) { PFN_xrGetActionStateVector2f fn=(PFN_xrGetActionStateVector2f)mr_sym("xrGetActionStateVector2f"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetCurrentInteractionProfile(
    XrSession                                   session,
    XrPath                                      topLevelUserPath,
    XrInteractionProfileState*                  interactionProfile) { PFN_xrGetCurrentInteractionProfile fn=(PFN_xrGetCurrentInteractionProfile)mr_sym("xrGetCurrentInteractionProfile"); return fn ? fn(session, topLevelUserPath, interactionProfile) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetInputSourceLocalizedName(
    XrSession                                   session,
    const XrInputSourceLocalizedNameGetInfo*    getInfo,
    uint32_t                                    bufferCapacityInput,
    uint32_t*                                   bufferCountOutput,
    char*                                       buffer) { PFN_xrGetInputSourceLocalizedName fn=(PFN_xrGetInputSourceLocalizedName)mr_sym("xrGetInputSourceLocalizedName"); return fn ? fn(session, getInfo, bufferCapacityInput, bufferCountOutput, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProperties(
    XrInstance                                  instance,
    XrInstanceProperties*                       instanceProperties) { PFN_xrGetInstanceProperties fn=(PFN_xrGetInstanceProperties)mr_sym("xrGetInstanceProperties"); return fn ? fn(instance, instanceProperties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetReferenceSpaceBoundsRect(
    XrSession                                   session,
    XrReferenceSpaceType                        referenceSpaceType,
    XrExtent2Df*                                bounds) { PFN_xrGetReferenceSpaceBoundsRect fn=(PFN_xrGetReferenceSpaceBoundsRect)mr_sym("xrGetReferenceSpaceBoundsRect"); return fn ? fn(session, referenceSpaceType, bounds) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetSystem(
    XrInstance                                  instance,
    const XrSystemGetInfo*                      getInfo,
    XrSystemId*                                 systemId) { PFN_xrGetSystem fn=(PFN_xrGetSystem)mr_sym("xrGetSystem"); return fn ? fn(instance, getInfo, systemId) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetSystemProperties(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrSystemProperties*                         properties) { PFN_xrGetSystemProperties fn=(PFN_xrGetSystemProperties)mr_sym("xrGetSystemProperties"); return fn ? fn(instance, systemId, properties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrGetViewConfigurationProperties(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrViewConfigurationType                     viewConfigurationType,
    XrViewConfigurationProperties*              configurationProperties) { PFN_xrGetViewConfigurationProperties fn=(PFN_xrGetViewConfigurationProperties)mr_sym("xrGetViewConfigurationProperties"); return fn ? fn(instance, systemId, viewConfigurationType, configurationProperties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrInitializeLoaderKHR(
    const XrLoaderInitInfoBaseHeaderKHR*        loaderInitInfo) { PFN_xrInitializeLoaderKHR fn=(PFN_xrInitializeLoaderKHR)mr_sym("xrInitializeLoaderKHR"); return fn ? fn(loaderInitInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrLocateSpace(
    XrSpace                                     space,
    XrSpace                                     baseSpace,
    XrTime                                      time,
    XrSpaceLocation*                            location) { PFN_xrLocateSpace fn=(PFN_xrLocateSpace)mr_sym("xrLocateSpace"); return fn ? fn(space, baseSpace, time, location) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrPathToString(
    XrInstance                                  instance,
    XrPath                                      path,
    uint32_t                                    bufferCapacityInput,
    uint32_t*                                   bufferCountOutput,
    char*                                       buffer) { PFN_xrPathToString fn=(PFN_xrPathToString)mr_sym("xrPathToString"); return fn ? fn(instance, path, bufferCapacityInput, bufferCountOutput, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrReleaseSwapchainImage(
    XrSwapchain                                 swapchain,
    const XrSwapchainImageReleaseInfo*          releaseInfo) { PFN_xrReleaseSwapchainImage fn=(PFN_xrReleaseSwapchainImage)mr_sym("xrReleaseSwapchainImage"); return fn ? fn(swapchain, releaseInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrRequestExitSession(
    XrSession                                   session) { PFN_xrRequestExitSession fn=(PFN_xrRequestExitSession)mr_sym("xrRequestExitSession"); return fn ? fn(session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrResultToString(
    XrInstance                                  instance,
    XrResult                                    value,
    char                                        buffer[XR_MAX_RESULT_STRING_SIZE]) { PFN_xrResultToString fn=(PFN_xrResultToString)mr_sym("xrResultToString"); return fn ? fn(instance, value, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrStopHapticFeedback(
    XrSession                                   session,
    const XrHapticActionInfo*                   hapticActionInfo) { PFN_xrStopHapticFeedback fn=(PFN_xrStopHapticFeedback)mr_sym("xrStopHapticFeedback"); return fn ? fn(session, hapticActionInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrStringToPath(
    XrInstance                                  instance,
    const char*                                 pathString,
    XrPath*                                     path) { PFN_xrStringToPath fn=(PFN_xrStringToPath)mr_sym("xrStringToPath"); return fn ? fn(instance, pathString, path) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrStructureTypeToString(
    XrInstance                                  instance,
    XrStructureType                             value,
    char                                        buffer[XR_MAX_STRUCTURE_NAME_SIZE]) { PFN_xrStructureTypeToString fn=(PFN_xrStructureTypeToString)mr_sym("xrStructureTypeToString"); return fn ? fn(instance, value, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrSyncActions(
    XrSession                                   session,
    const XrActionsSyncInfo*                    syncInfo) { PFN_xrSyncActions fn=(PFN_xrSyncActions)mr_sym("xrSyncActions"); return fn ? fn(session, syncInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrWaitFrame(
    XrSession                                   session,
    const XrFrameWaitInfo*                      frameWaitInfo,
    XrFrameState*                               frameState) { PFN_xrWaitFrame fn=(PFN_xrWaitFrame)mr_sym("xrWaitFrame"); return fn ? fn(session, frameWaitInfo, frameState) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrWaitSwapchainImage(
    XrSwapchain                                 swapchain,
    const XrSwapchainImageWaitInfo*             waitInfo) { PFN_xrWaitSwapchainImage fn=(PFN_xrWaitSwapchainImage)mr_sym("xrWaitSwapchainImage"); return fn ? fn(swapchain, waitInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
XRAPI_ATTR XrResult XRAPI_CALL xrLocateViews(XrSession s,const XrViewLocateInfo* i,XrViewState* st,uint32_t cap,uint32_t* count,XrView* v) {
 PFN_xrLocateViews fn=(PFN_xrLocateViews)mr_sym("xrLocateViews");return fn?fn(s,i,st,cap,count,v):XR_ERROR_FUNCTION_UNSUPPORTED;
}
XRAPI_ATTR XrResult XRAPI_CALL xrPollEvent(XrInstance i,XrEventDataBuffer* out) {
 PFN_xrVoidFunction fn=NULL;XrResult r=xrGetInstanceProcAddr(i,"xrPollEvent",&fn);
 return XR_SUCCEEDED(r)&&fn?((PFN_xrPollEvent)fn)(i,out):r;
}
