#include <openxr/openxr.h>
#include <cstring>
static float last=123;
static const XrFrameEndInfo* endInfo=nullptr;
extern "C" XrResult xrCreateInstance(const XrInstanceCreateInfo*,XrInstance* instance){*instance=(XrInstance)1;return XR_SUCCESS;}
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
    else if(!strcmp(name,"spaceWarpSentinel"))*fn=spaceWarpSentinel;
    else {*fn=nullptr;return XR_ERROR_FUNCTION_UNSUPPORTED;}return XR_SUCCESS;
}
