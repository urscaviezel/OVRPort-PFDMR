#include <openxr/openxr.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdlib>
static void check(bool ok,const char* message){if(!ok){fprintf(stderr,"FAIL %s\n",message);exit(1);}printf("PASS %s\n",message);}
int main(){
    void* wrapper=dlopen("./libopenxr_loader.so",RTLD_NOW|RTLD_LOCAL);check(wrapper,"load refresh wrapper");
    auto request=(PFN_xrRequestDisplayRefreshRateFB)dlsym(wrapper,"xrRequestDisplayRefreshRateFB");
    check(request((XrSession)1,90)==XR_SESSION_LOSS_PENDING,"direct request preserves runtime result");
    void* original=dlopen("libpfdmr_refresh_original.so",RTLD_NOW|RTLD_LOCAL);
    auto recorded=(float(*)())dlsym(original,"recordedRate");check(recorded&&recorded()==0,"90 Hz preference replaced by system choice");
    auto get=(PFN_xrGetInstanceProcAddr)dlsym(wrapper,"xrGetInstanceProcAddr");PFN_xrVoidFunction fn=nullptr;
    check(get((XrInstance)1,"xrRequestDisplayRefreshRateFB",&fn)==XR_SUCCESS&&fn==(PFN_xrVoidFunction)request,"resolver returns intercepted request");
    check(((PFN_xrRequestDisplayRefreshRateFB)fn)((XrSession)1,72)==XR_SESSION_LOSS_PENDING&&recorded()==0,"72 Hz request also leaves choice to runtime");
    check(request(XR_NULL_HANDLE,90)==XR_ERROR_HANDLE_INVALID,"runtime errors preserved");
    get((XrInstance)1,"xrGetDisplayRefreshRateFB",&fn);float rate=0;
    check(((PFN_xrGetDisplayRefreshRateFB)fn)((XrSession)1,&rate)==XR_SUCCESS&&rate==80,"actual refresh rate query unchanged");
    get((XrInstance)1,"spaceWarpSentinel",&fn);check(fn==(PFN_xrVoidFunction)dlsym(original,"spaceWarpSentinel"),"unrelated extension unchanged");
    get((XrInstance)1,"xrGetInstanceProcAddr",&fn);check(fn==(PFN_xrVoidFunction)get,"resolver lookup retains wrapper");
    check(get((XrInstance)1,"unsupported",&fn)==XR_ERROR_FUNCTION_UNSUPPORTED&&!fn,"unsupported extension unchanged");
    XrCompositionLayerSpaceWarpInfoFB spacewarp{XR_TYPE_COMPOSITION_LAYER_SPACE_WARP_INFO_FB};
    XrFrameEndInfo info{XR_TYPE_FRAME_END_INFO};info.next=&spacewarp;
    auto end=(PFN_xrEndFrame)dlsym(wrapper,"xrEndFrame");
    auto recordedEnd=(const XrFrameEndInfo*(*)())dlsym(original,"recordedEnd");
    check(end((XrSession)1,&info)==XR_SUCCESS&&recordedEnd()==&info&&info.next==&spacewarp,"frame and SpaceWarp data forwarded untouched");
    check(dlsym(dlopen("libopenxr_loader.so",RTLD_NOW|RTLD_LOCAL),"xrRequestDisplayRefreshRateFB")==reinterpret_cast<void*>(request),"reopened loader retains refresh wrapper");
}
