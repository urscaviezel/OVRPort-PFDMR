#include <openxr/openxr.h>
#include <cstring>
#include <cstdint>
extern "C" {
XrResult mockResult=XR_SUCCESS;
uint64_t mockAction=4;
XrActionSpaceCreateInfo mockSpace{};
void (*mockFrame)(const XrFrameEndInfo*)=nullptr;
XrResult xrCreateInstance(const XrInstanceCreateInfo*,XrInstance* out){*out=(XrInstance)1;return mockResult;}
XrResult xrDestroyInstance(XrInstance){return mockResult;}
XrResult xrCreateSession(XrInstance,const XrSessionCreateInfo*,XrSession* out){*out=(XrSession)2;return mockResult;}
XrResult xrDestroySession(XrSession){return mockResult;}
XrResult xrEndSession(XrSession){return mockResult;}
XrResult xrCreateActionSet(XrInstance,const XrActionSetCreateInfo*,XrActionSet* out){*out=(XrActionSet)3;return mockResult;}
XrResult xrDestroyActionSet(XrActionSet){return mockResult;}
XrResult xrCreateAction(XrActionSet,const XrActionCreateInfo*,XrAction* out){*out=(XrAction)mockAction;return mockResult;}
XrResult xrDestroyAction(XrAction){return mockResult;}
XrResult xrSuggestInteractionProfileBindings(XrInstance,const XrInteractionProfileSuggestedBinding*){return mockResult;}
XrResult xrCreateActionSpace(XrSession,const XrActionSpaceCreateInfo* info,XrSpace* out){if(info)mockSpace=*info;*out=(XrSpace)5;return mockResult;}
XrResult xrEndFrame(XrSession,const XrFrameEndInfo* info){if(mockFrame)mockFrame(info);return mockResult;}
XrResult xrLocateViews(XrSession,const XrViewLocateInfo*,XrViewState*,uint32_t,uint32_t*,XrView*){return mockResult;}
XrResult xrPathToString(XrInstance,XrPath path,uint32_t cap,uint32_t* count,char* out){const char* s=path==10?"/interaction_profiles/oculus/touch_controller":path==11?"/user/hand/left/input/grip/pose":path==13?"/interaction_profiles/khr/simple_controller":"/user/head/input/pose";*count=strlen(s)+1;if(cap<*count)return XR_ERROR_SIZE_INSUFFICIENT;strcpy(out,s);return XR_SUCCESS;}
XrResult xrGetInstanceProcAddr(XrInstance,const char* name,PFN_xrVoidFunction* out){
#define F(n) if(!strcmp(name,#n)){*out=(PFN_xrVoidFunction)n;return XR_SUCCESS;}
F(xrGetInstanceProcAddr) F(xrCreateInstance) F(xrDestroyInstance) F(xrCreateSession) F(xrDestroySession) F(xrEndSession) F(xrEndFrame) F(xrCreateActionSet) F(xrDestroyActionSet) F(xrCreateAction) F(xrDestroyAction) F(xrSuggestInteractionProfileBindings) F(xrCreateActionSpace) F(xrLocateViews) F(xrPathToString)
#undef F
*out=nullptr;return XR_ERROR_FUNCTION_UNSUPPORTED;
}
}
