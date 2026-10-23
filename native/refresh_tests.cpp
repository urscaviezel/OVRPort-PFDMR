#include <openxr/openxr.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdlib>
#include <initializer_list>
#include "mock_refresh_jni.h"
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
    struct AndroidInfo{XrStructureType type;const void* next;void* vm;void* context;};
    AndroidInfo android{XR_TYPE_INSTANCE_CREATE_INFO_ANDROID_KHR,nullptr,mockJavaVm(),reinterpret_cast<void*>(1)};
    XrInstanceCreateInfo createInfo{XR_TYPE_INSTANCE_CREATE_INFO};createInfo.next=&android;XrInstance instance;
    auto create=(PFN_xrCreateInstance)dlsym(wrapper,"xrCreateInstance");
    check(create(&createInfo,&instance)==XR_SUCCESS,"capture Android context without changing instance creation");
    for(int hz:{72,80,90}){mockHz=hz;check(request((XrSession)1,90)==XR_SESSION_LOSS_PENDING&&recorded()==hz,"forward current HMD selection, including changes after startup");}
    mockHz=999;request((XrSession)1,90);check(recorded()==0,"unknown setting falls back to runtime preference");
    mockHz=72;mockFail=true;request((XrSession)1,90);check(recorded()==0&&!mockException,"settings exception cleared with safe fallback");
    mockFail=false;mockHz=72;
    auto enabled=(unsigned(*)())dlsym(original,"enabledRefreshCount");
    check(enabled()==1,"enable supported refresh extension for apps without preference");
    const char* extensions[]={XR_FB_DISPLAY_REFRESH_RATE_EXTENSION_NAME};createInfo.enabledExtensionCount=1;createInfo.enabledExtensionNames=extensions;
    create(&createInfo,&instance);check(enabled()==1,"do not duplicate enabled refresh extension");
    get(instance,"xrCreateSession",&fn);auto createSession=(PFN_xrCreateSession)fn;
    XrSession session;XrSessionCreateInfo sessionInfo{XR_TYPE_SESSION_CREATE_INFO};
    check(createSession(instance,&sessionInfo,&session)==XR_SUCCESS,"resolver session creation succeeds");
    get(instance,"xrBeginSession",&fn);auto beginSession=(PFN_xrBeginSession)fn;
    XrSessionBeginInfo beginInfo{XR_TYPE_SESSION_BEGIN_INFO};
    check(beginSession(session,&beginInfo)==XR_SUCCESS&&recorded()==72,"apply HMD preference after session start without app request");
    mockHz=80;check(beginSession(session,&beginInfo)==XR_SUCCESS&&recorded()==80,"reapply current preference on session restart");
    check(beginSession(XR_NULL_HANDLE,&beginInfo)==XR_ERROR_HANDLE_INVALID&&recorded()==90,"failed session start does not request rate");
    mockHz=999;check(beginSession(session,&beginInfo)==XR_SUCCESS&&recorded()==90,"missing preference leaves startup rate untouched");
    get(instance,"xrDestroySession",&fn);check(((PFN_xrDestroySession)fn)(session)==XR_SUCCESS,"session destruction forwarded");
    mockHz=72;beginSession(session,&beginInfo);check(recorded()==90,"destroyed session no longer carries refresh dispatch");
}
