#include <openxr/openxr.h>
extern void* sym(const char*);
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrAcquireSwapchainImage(
    XrSwapchain                                 swapchain,
    const XrSwapchainImageAcquireInfo*          acquireInfo,
    uint32_t*                                   index) { auto fn=(PFN_xrAcquireSwapchainImage)sym("xrAcquireSwapchainImage"); return fn ? fn(swapchain, acquireInfo, index) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrApplyHapticFeedback(
    XrSession                                   session,
    const XrHapticActionInfo*                   hapticActionInfo,
    const XrHapticBaseHeader*                   hapticFeedback) { auto fn=(PFN_xrApplyHapticFeedback)sym("xrApplyHapticFeedback"); return fn ? fn(session, hapticActionInfo, hapticFeedback) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrAttachSessionActionSets(
    XrSession                                   session,
    const XrSessionActionSetsAttachInfo*        attachInfo) { auto fn=(PFN_xrAttachSessionActionSets)sym("xrAttachSessionActionSets"); return fn ? fn(session, attachInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrBeginFrame(
    XrSession                                   session,
    const XrFrameBeginInfo*                     frameBeginInfo) { auto fn=(PFN_xrBeginFrame)sym("xrBeginFrame"); return fn ? fn(session, frameBeginInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrBeginSession(
    XrSession                                   session,
    const XrSessionBeginInfo*                   beginInfo) { auto fn=(PFN_xrBeginSession)sym("xrBeginSession"); return fn ? fn(session, beginInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateAction(
    XrActionSet                                 actionSet,
    const XrActionCreateInfo*                   createInfo,
    XrAction*                                   action) { auto fn=(PFN_xrCreateAction)sym("xrCreateAction"); return fn ? fn(actionSet, createInfo, action) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateActionSet(
    XrInstance                                  instance,
    const XrActionSetCreateInfo*                createInfo,
    XrActionSet*                                actionSet) { auto fn=(PFN_xrCreateActionSet)sym("xrCreateActionSet"); return fn ? fn(instance, createInfo, actionSet) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateActionSpace(
    XrSession                                   session,
    const XrActionSpaceCreateInfo*              createInfo,
    XrSpace*                                    space) { auto fn=(PFN_xrCreateActionSpace)sym("xrCreateActionSpace"); return fn ? fn(session, createInfo, space) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateInstance(
    const XrInstanceCreateInfo*                 createInfo,
    XrInstance*                                 instance) { auto fn=(PFN_xrCreateInstance)sym("xrCreateInstance"); return fn ? fn(createInfo, instance) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateReferenceSpace(
    XrSession                                   session,
    const XrReferenceSpaceCreateInfo*           createInfo,
    XrSpace*                                    space) { auto fn=(PFN_xrCreateReferenceSpace)sym("xrCreateReferenceSpace"); return fn ? fn(session, createInfo, space) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateSession(
    XrInstance                                  instance,
    const XrSessionCreateInfo*                  createInfo,
    XrSession*                                  session) { auto fn=(PFN_xrCreateSession)sym("xrCreateSession"); return fn ? fn(instance, createInfo, session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateSwapchain(
    XrSession                                   session,
    const XrSwapchainCreateInfo*                createInfo,
    XrSwapchain*                                swapchain) { auto fn=(PFN_xrCreateSwapchain)sym("xrCreateSwapchain"); return fn ? fn(session, createInfo, swapchain) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroyAction(
    XrAction                                    action) { auto fn=(PFN_xrDestroyAction)sym("xrDestroyAction"); return fn ? fn(action) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroyActionSet(
    XrActionSet                                 actionSet) { auto fn=(PFN_xrDestroyActionSet)sym("xrDestroyActionSet"); return fn ? fn(actionSet) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroyInstance(
    XrInstance                                  instance) { auto fn=(PFN_xrDestroyInstance)sym("xrDestroyInstance"); return fn ? fn(instance) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroySession(
    XrSession                                   session) { auto fn=(PFN_xrDestroySession)sym("xrDestroySession"); return fn ? fn(session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroySpace(
    XrSpace                                     space) { auto fn=(PFN_xrDestroySpace)sym("xrDestroySpace"); return fn ? fn(space) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroySwapchain(
    XrSwapchain                                 swapchain) { auto fn=(PFN_xrDestroySwapchain)sym("xrDestroySwapchain"); return fn ? fn(swapchain) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEndFrame(
    XrSession                                   session,
    const XrFrameEndInfo*                       frameEndInfo) { auto fn=(PFN_xrEndFrame)sym("xrEndFrame"); return fn ? fn(session, frameEndInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEndSession(
    XrSession                                   session) { auto fn=(PFN_xrEndSession)sym("xrEndSession"); return fn ? fn(session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateApiLayerProperties(
    uint32_t                                    propertyCapacityInput,
    uint32_t*                                   propertyCountOutput,
    XrApiLayerProperties*                       properties) { auto fn=(PFN_xrEnumerateApiLayerProperties)sym("xrEnumerateApiLayerProperties"); return fn ? fn(propertyCapacityInput, propertyCountOutput, properties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateBoundSourcesForAction(
    XrSession                                   session,
    const XrBoundSourcesForActionEnumerateInfo* enumerateInfo,
    uint32_t                                    sourceCapacityInput,
    uint32_t*                                   sourceCountOutput,
    XrPath*                                     sources) { auto fn=(PFN_xrEnumerateBoundSourcesForAction)sym("xrEnumerateBoundSourcesForAction"); return fn ? fn(session, enumerateInfo, sourceCapacityInput, sourceCountOutput, sources) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateEnvironmentBlendModes(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrViewConfigurationType                     viewConfigurationType,
    uint32_t                                    environmentBlendModeCapacityInput,
    uint32_t*                                   environmentBlendModeCountOutput,
    XrEnvironmentBlendMode*                     environmentBlendModes) { auto fn=(PFN_xrEnumerateEnvironmentBlendModes)sym("xrEnumerateEnvironmentBlendModes"); return fn ? fn(instance, systemId, viewConfigurationType, environmentBlendModeCapacityInput, environmentBlendModeCountOutput, environmentBlendModes) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateInstanceExtensionProperties(
    const char*                                 layerName,
    uint32_t                                    propertyCapacityInput,
    uint32_t*                                   propertyCountOutput,
    XrExtensionProperties*                      properties) { auto fn=(PFN_xrEnumerateInstanceExtensionProperties)sym("xrEnumerateInstanceExtensionProperties"); return fn ? fn(layerName, propertyCapacityInput, propertyCountOutput, properties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateReferenceSpaces(
    XrSession                                   session,
    uint32_t                                    spaceCapacityInput,
    uint32_t*                                   spaceCountOutput,
    XrReferenceSpaceType*                       spaces) { auto fn=(PFN_xrEnumerateReferenceSpaces)sym("xrEnumerateReferenceSpaces"); return fn ? fn(session, spaceCapacityInput, spaceCountOutput, spaces) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateSwapchainFormats(
    XrSession                                   session,
    uint32_t                                    formatCapacityInput,
    uint32_t*                                   formatCountOutput,
    int64_t*                                    formats) { auto fn=(PFN_xrEnumerateSwapchainFormats)sym("xrEnumerateSwapchainFormats"); return fn ? fn(session, formatCapacityInput, formatCountOutput, formats) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateSwapchainImages(
    XrSwapchain                                 swapchain,
    uint32_t                                    imageCapacityInput,
    uint32_t*                                   imageCountOutput,
    XrSwapchainImageBaseHeader*                 images) { auto fn=(PFN_xrEnumerateSwapchainImages)sym("xrEnumerateSwapchainImages"); return fn ? fn(swapchain, imageCapacityInput, imageCountOutput, images) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateViewConfigurationViews(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrViewConfigurationType                     viewConfigurationType,
    uint32_t                                    viewCapacityInput,
    uint32_t*                                   viewCountOutput,
    XrViewConfigurationView*                    views) { auto fn=(PFN_xrEnumerateViewConfigurationViews)sym("xrEnumerateViewConfigurationViews"); return fn ? fn(instance, systemId, viewConfigurationType, viewCapacityInput, viewCountOutput, views) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEnumerateViewConfigurations(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    uint32_t                                    viewConfigurationTypeCapacityInput,
    uint32_t*                                   viewConfigurationTypeCountOutput,
    XrViewConfigurationType*                    viewConfigurationTypes) { auto fn=(PFN_xrEnumerateViewConfigurations)sym("xrEnumerateViewConfigurations"); return fn ? fn(instance, systemId, viewConfigurationTypeCapacityInput, viewConfigurationTypeCountOutput, viewConfigurationTypes) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStateBoolean(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStateBoolean*                       state) { auto fn=(PFN_xrGetActionStateBoolean)sym("xrGetActionStateBoolean"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStateFloat(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStateFloat*                         state) { auto fn=(PFN_xrGetActionStateFloat)sym("xrGetActionStateFloat"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStatePose(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStatePose*                          state) { auto fn=(PFN_xrGetActionStatePose)sym("xrGetActionStatePose"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetActionStateVector2f(
    XrSession                                   session,
    const XrActionStateGetInfo*                 getInfo,
    XrActionStateVector2f*                      state) { auto fn=(PFN_xrGetActionStateVector2f)sym("xrGetActionStateVector2f"); return fn ? fn(session, getInfo, state) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetCurrentInteractionProfile(
    XrSession                                   session,
    XrPath                                      topLevelUserPath,
    XrInteractionProfileState*                  interactionProfile) { auto fn=(PFN_xrGetCurrentInteractionProfile)sym("xrGetCurrentInteractionProfile"); return fn ? fn(session, topLevelUserPath, interactionProfile) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetInputSourceLocalizedName(
    XrSession                                   session,
    const XrInputSourceLocalizedNameGetInfo*    getInfo,
    uint32_t                                    bufferCapacityInput,
    uint32_t*                                   bufferCountOutput,
    char*                                       buffer) { auto fn=(PFN_xrGetInputSourceLocalizedName)sym("xrGetInputSourceLocalizedName"); return fn ? fn(session, getInfo, bufferCapacityInput, bufferCountOutput, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProperties(
    XrInstance                                  instance,
    XrInstanceProperties*                       instanceProperties) { auto fn=(PFN_xrGetInstanceProperties)sym("xrGetInstanceProperties"); return fn ? fn(instance, instanceProperties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetReferenceSpaceBoundsRect(
    XrSession                                   session,
    XrReferenceSpaceType                        referenceSpaceType,
    XrExtent2Df*                                bounds) { auto fn=(PFN_xrGetReferenceSpaceBoundsRect)sym("xrGetReferenceSpaceBoundsRect"); return fn ? fn(session, referenceSpaceType, bounds) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetSystem(
    XrInstance                                  instance,
    const XrSystemGetInfo*                      getInfo,
    XrSystemId*                                 systemId) { auto fn=(PFN_xrGetSystem)sym("xrGetSystem"); return fn ? fn(instance, getInfo, systemId) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetSystemProperties(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrSystemProperties*                         properties) { auto fn=(PFN_xrGetSystemProperties)sym("xrGetSystemProperties"); return fn ? fn(instance, systemId, properties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetViewConfigurationProperties(
    XrInstance                                  instance,
    XrSystemId                                  systemId,
    XrViewConfigurationType                     viewConfigurationType,
    XrViewConfigurationProperties*              configurationProperties) { auto fn=(PFN_xrGetViewConfigurationProperties)sym("xrGetViewConfigurationProperties"); return fn ? fn(instance, systemId, viewConfigurationType, configurationProperties) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrInitializeLoaderKHR(
    const XrLoaderInitInfoBaseHeaderKHR*        loaderInitInfo) { auto fn=(PFN_xrInitializeLoaderKHR)sym("xrInitializeLoaderKHR"); return fn ? fn(loaderInitInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrLocateSpace(
    XrSpace                                     space,
    XrSpace                                     baseSpace,
    XrTime                                      time,
    XrSpaceLocation*                            location) { auto fn=(PFN_xrLocateSpace)sym("xrLocateSpace"); return fn ? fn(space, baseSpace, time, location) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrPathToString(
    XrInstance                                  instance,
    XrPath                                      path,
    uint32_t                                    bufferCapacityInput,
    uint32_t*                                   bufferCountOutput,
    char*                                       buffer) { auto fn=(PFN_xrPathToString)sym("xrPathToString"); return fn ? fn(instance, path, bufferCapacityInput, bufferCountOutput, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrPollEvent(
    XrInstance                                  instance,
    XrEventDataBuffer*                          eventData) { auto fn=(PFN_xrPollEvent)sym("xrPollEvent"); return fn ? fn(instance, eventData) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrReleaseSwapchainImage(
    XrSwapchain                                 swapchain,
    const XrSwapchainImageReleaseInfo*          releaseInfo) { auto fn=(PFN_xrReleaseSwapchainImage)sym("xrReleaseSwapchainImage"); return fn ? fn(swapchain, releaseInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrRequestExitSession(
    XrSession                                   session) { auto fn=(PFN_xrRequestExitSession)sym("xrRequestExitSession"); return fn ? fn(session) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrResultToString(
    XrInstance                                  instance,
    XrResult                                    value,
    char                                        buffer[XR_MAX_RESULT_STRING_SIZE]) { auto fn=(PFN_xrResultToString)sym("xrResultToString"); return fn ? fn(instance, value, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrStopHapticFeedback(
    XrSession                                   session,
    const XrHapticActionInfo*                   hapticActionInfo) { auto fn=(PFN_xrStopHapticFeedback)sym("xrStopHapticFeedback"); return fn ? fn(session, hapticActionInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrStringToPath(
    XrInstance                                  instance,
    const char*                                 pathString,
    XrPath*                                     path) { auto fn=(PFN_xrStringToPath)sym("xrStringToPath"); return fn ? fn(instance, pathString, path) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrStructureTypeToString(
    XrInstance                                  instance,
    XrStructureType                             value,
    char                                        buffer[XR_MAX_STRUCTURE_NAME_SIZE]) { auto fn=(PFN_xrStructureTypeToString)sym("xrStructureTypeToString"); return fn ? fn(instance, value, buffer) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrSuggestInteractionProfileBindings(
    XrInstance                                  instance,
    const XrInteractionProfileSuggestedBinding* suggestedBindings) { auto fn=(PFN_xrSuggestInteractionProfileBindings)sym("xrSuggestInteractionProfileBindings"); return fn ? fn(instance, suggestedBindings) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrSyncActions(
    XrSession                                   session,
    const XrActionsSyncInfo*                    syncInfo) { auto fn=(PFN_xrSyncActions)sym("xrSyncActions"); return fn ? fn(session, syncInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrWaitFrame(
    XrSession                                   session,
    const XrFrameWaitInfo*                      frameWaitInfo,
    XrFrameState*                               frameState) { auto fn=(PFN_xrWaitFrame)sym("xrWaitFrame"); return fn ? fn(session, frameWaitInfo, frameState) : XR_ERROR_FUNCTION_UNSUPPORTED; }
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrWaitSwapchainImage(
    XrSwapchain                                 swapchain,
    const XrSwapchainImageWaitInfo*             waitInfo) { auto fn=(PFN_xrWaitSwapchainImage)sym("xrWaitSwapchainImage"); return fn ? fn(swapchain, waitInfo) : XR_ERROR_FUNCTION_UNSUPPORTED; }