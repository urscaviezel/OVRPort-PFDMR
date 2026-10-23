// Optional system-preferred display refresh rate. GPL-3.0.
#include <openxr/openxr.h>
#include <dlfcn.h>
#include <atomic>
#include <cstring>

extern "C" __attribute__((used,visibility("default"))) const char pfdmrRefreshMarker[]="PFDMR_SYSTEM_REFRESH_V1";
void* sym(const char* name) {
    static void* library=dlopen("libpfdmr_refresh_original.so",RTLD_NOW|RTLD_LOCAL);
    return library?dlsym(library,name):nullptr;
}
static std::atomic<PFN_xrRequestDisplayRefreshRateFB> requestRate{nullptr};
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrRequestDisplayRefreshRateFB(XrSession session,float requested) {
    auto fn=requestRate.load();
    if(!fn)fn=reinterpret_cast<PFN_xrRequestDisplayRefreshRateFB>(sym("xrRequestDisplayRefreshRateFB"));
    // OpenXR: 0 Hz means the application has no preference. The runtime chooses
    // its system rate. Never forge the actual-rate query, events or frame timing.
    return fn?fn(session,0.0f):XR_ERROR_FUNCTION_UNSUPPORTED;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrLocateViews(XrSession session,const XrViewLocateInfo* info,
    XrViewState* state,uint32_t capacity,uint32_t* count,XrView* views) {
    auto fn=reinterpret_cast<PFN_xrLocateViews>(sym("xrLocateViews"));
    return fn?fn(session,info,state,capacity,count,views):XR_ERROR_FUNCTION_UNSUPPORTED;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProcAddr(XrInstance instance,const char* name,PFN_xrVoidFunction* fn) {
    auto get=reinterpret_cast<PFN_xrGetInstanceProcAddr>(sym("xrGetInstanceProcAddr"));
    if(!get)return XR_ERROR_INITIALIZATION_FAILED;
    XrResult result=get(instance,name,fn);
    if(XR_SUCCEEDED(result)&&name&&fn&&*fn) {
        if(!strcmp(name,"xrRequestDisplayRefreshRateFB")) {
            requestRate.store(reinterpret_cast<PFN_xrRequestDisplayRefreshRateFB>(*fn));
            *fn=reinterpret_cast<PFN_xrVoidFunction>(xrRequestDisplayRefreshRateFB);
        } else if(!strcmp(name,"xrGetInstanceProcAddr")) *fn=reinterpret_cast<PFN_xrVoidFunction>(xrGetInstanceProcAddr);
    }
    return result;
}
