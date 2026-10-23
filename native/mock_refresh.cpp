#include <openxr/openxr.h>
#include <cstring>
static float last=123;
static const XrFrameEndInfo* endInfo=nullptr;
static unsigned extensionCount=0;
extern "C" unsigned enabledRefreshCount(){return extensionCount;}
extern "C" XrResult xrEnumerateInstanceExtensionProperties(const char*,uint32_t capacity,uint32_t* count,XrExtensionProperties* properties){*count=1;if(capacity)strcpy(properties[0].extensionName,XR_FB_DISPLAY_REFRESH_RATE_EXTENSION_NAME);return XR_SUCCESS;}
extern "C" XrResult xrCreateInstance(const XrInstanceCreateInfo* info,XrInstance* instance){extensionCount=0;for(uint32_t i=0;i<info->enabledExtensionCount;i++)if(!strcmp(info->enabledExtensionNames[i],XR_FB_DISPLAY_REFRESH_RATE_EXTENSION_NAME))extensionCount++;*instance=(XrInstance)1;return XR_SUCCESS;}
extern "C" XrResult xrCreateSession(XrInstance,const XrSessionCreateInfo*,XrSession* session){*session=(XrSession)2;return XR_SUCCESS;}
extern "C" XrResult xrBeginSession(XrSession session,const XrSessionBeginInfo*){last=90;return session?XR_SUCCESS:XR_ERROR_HANDLE_INVALID;}
extern "C" XrResult xrDestroySession(XrSession){return XR_SUCCESS;}

extern "C" float recordedRate(){return last;}
extern "C" const XrFrameEndInfo* recordedEnd(){return endInfo;}
extern "C" XrResult xrRequestDisplayRefreshRateFB(XrSession session,float rate){last=rate;return session?XR_SESSION_LOSS_PENDING:XR_ERROR_HANDLE_INVALID;}
extern "C" XrResult xrGetDisplayRefreshRateFB(XrSession,float* rate){*rate=80;return XR_SUCCESS;}
extern "C" XrResult xrEndFrame(XrSession,const XrFrameEndInfo* info){endInfo=info;return XR_SUCCESS;}
extern "C" void spaceWarpSentinel(){}
extern "C" XrResult xrGetInstanceProcAddr(XrInstance,const char* name,PFN_xrVoidFunction* fn){
    if(!strcmp(name,"xrRequestDisplayRefreshRateFB"))*fn=(PFN_xrVoidFunction)xrRequestDisplayRefreshRateFB;
    else if(!strcmp(name,"xrGetDisplayRefreshRateFB"))*fn=(PFN_xrVoidFunction)xrGetDisplayRefreshRateFB;
    else if(!strcmp(name,"xrGetInstanceProcAddr"))*fn=(PFN_xrVoidFunction)xrGetInstanceProcAddr;
    else if(!strcmp(name,"xrCreateSession"))*fn=(PFN_xrVoidFunction)xrCreateSession;
    else if(!strcmp(name,"xrBeginSession"))*fn=(PFN_xrVoidFunction)xrBeginSession;
    else if(!strcmp(name,"xrDestroySession"))*fn=(PFN_xrVoidFunction)xrDestroySession;
    else if(!strcmp(name,"spaceWarpSentinel"))*fn=spaceWarpSentinel;
    else {*fn=nullptr;return XR_ERROR_FUNCTION_UNSUPPORTED;}return XR_SUCCESS;
}
