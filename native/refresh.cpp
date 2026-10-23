// Optional system-preferred display refresh rate. GPL-3.0.
#include <openxr/openxr.h>
#include <dlfcn.h>
#include <atomic>
#include <cstring>
#include <jni.h>
#include <mutex>
#include <android/log.h>

// Layouts from XR_KHR_android_create_instance / XR_KHR_loader_init_android.
struct AndroidContextInfo { XrStructureType type; const void* next; void* vm; void* context; };
static std::mutex contextMutex;
static JavaVM* appVm=nullptr;
static jobject appContext=nullptr;
static void captureContext(const void* chain,XrStructureType type) {
    auto info=static_cast<const AndroidContextInfo*>(chain);
    for(int n=0;info&&n<64;n++,info=static_cast<const AndroidContextInfo*>(info->next)) {
        if(info->type!=type)continue;
        if(!info->vm||!info->context)return;
        std::lock_guard<std::mutex> lock(contextMutex);
        if(appContext)return;
        auto vm=static_cast<JavaVM*>(info->vm);JNIEnv* env=nullptr;bool attached=false;
        if(vm->GetEnv(reinterpret_cast<void**>(&env),JNI_VERSION_1_6)!=JNI_OK){
            if(vm->AttachCurrentThread(&env,nullptr)!=JNI_OK)return;attached=true;
        }
        appContext=env->NewGlobalRef(static_cast<jobject>(info->context));
        if(env->ExceptionCheck()){env->ExceptionClear();appContext=nullptr;}
        if(appContext)appVm=vm;
        if(attached)vm->DetachCurrentThread();return;
    }
}
static float headsetRate() {
    std::lock_guard<std::mutex> lock(contextMutex);
    if(!appVm||!appContext)return 0;
    JNIEnv* env=nullptr;bool attached=false;float result=0;
    if(appVm->GetEnv(reinterpret_cast<void**>(&env),JNI_VERSION_1_6)!=JNI_OK){
        if(appVm->AttachCurrentThread(&env,nullptr)!=JNI_OK)return 0;attached=true;
    }
    if(env->PushLocalFrame(8)==JNI_OK){
        auto read=[&]()->int {
            jclass context=env->GetObjectClass(appContext);if(env->ExceptionCheck()||!context)return 0;
            jmethodID get=env->GetMethodID(context,"getContentResolver","()Landroid/content/ContentResolver;");if(env->ExceptionCheck()||!get)return 0;
            jobject resolver=env->CallObjectMethod(appContext,get);if(env->ExceptionCheck()||!resolver)return 0;
            jclass settings=env->FindClass("android/provider/Settings$System");if(env->ExceptionCheck()||!settings)return 0;
            jmethodID readInt=env->GetStaticMethodID(settings,"getInt","(Landroid/content/ContentResolver;Ljava/lang/String;I)I");if(env->ExceptionCheck()||!readInt)return 0;
            jstring key=env->NewStringUTF("SpatialHome_SystemFrequencyMode");if(env->ExceptionCheck()||!key)return 0;
            return env->CallStaticIntMethod(settings,readInt,resolver,key,0);
        };
        int rate=read();
        if(!env->ExceptionCheck()&&(rate==72||rate==80||rate==90))result=static_cast<float>(rate);
        if(env->ExceptionCheck())env->ExceptionClear();env->PopLocalFrame(nullptr);
    } else if(env->ExceptionCheck())env->ExceptionClear();
    if(attached)appVm->DetachCurrentThread();return result;
}

extern "C" __attribute__((used,visibility("default"))) const char pfdmrRefreshMarker[]="PFDMR_SYSTEM_REFRESH_V1";
void* sym(const char* name) {
    static void* library=dlopen("libpfdmr_refresh_original.so",RTLD_NOW|RTLD_LOCAL);
    return library?dlsym(library,name):nullptr;
}
static std::atomic<PFN_xrRequestDisplayRefreshRateFB> requestRate{nullptr};
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateInstance(const XrInstanceCreateInfo* info,XrInstance* instance) {
    if(info)captureContext(info->next,XR_TYPE_INSTANCE_CREATE_INFO_ANDROID_KHR);
    auto fn=reinterpret_cast<PFN_xrCreateInstance>(sym("xrCreateInstance"));
    return fn?fn(info,instance):XR_ERROR_FUNCTION_UNSUPPORTED;
}
static std::atomic<PFN_xrInitializeLoaderKHR> initializeLoader{nullptr};
static XrResult initLoader(const XrLoaderInitInfoBaseHeaderKHR* info) {
    captureContext(info,XR_TYPE_LOADER_INIT_INFO_ANDROID_KHR);
    auto fn=initializeLoader.load();return fn?fn(info):XR_ERROR_FUNCTION_UNSUPPORTED;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrRequestDisplayRefreshRateFB(XrSession session,float requested) {
    auto fn=requestRate.load();
    if(!fn)fn=reinterpret_cast<PFN_xrRequestDisplayRefreshRateFB>(sym("xrRequestDisplayRefreshRateFB"));
    float target=headsetRate();
    static std::atomic<int> logged{-1};int previous=logged.exchange(static_cast<int>(target));
    if(previous!=static_cast<int>(target))__android_log_print(ANDROID_LOG_INFO,"PFDMRRefresh","HMD setting %.0f Hz; app requested %.0f Hz%s",target,requested,target?"":"; setting unavailable, using runtime preference");
    return fn?fn(session,target):XR_ERROR_FUNCTION_UNSUPPORTED;
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
        } else if(!strcmp(name,"xrCreateInstance")) *fn=reinterpret_cast<PFN_xrVoidFunction>(xrCreateInstance);
        else if(!strcmp(name,"xrInitializeLoaderKHR")){initializeLoader=reinterpret_cast<PFN_xrInitializeLoaderKHR>(*fn);*fn=reinterpret_cast<PFN_xrVoidFunction>(initLoader);}
        else if(!strcmp(name,"xrGetInstanceProcAddr")) *fn=reinterpret_cast<PFN_xrVoidFunction>(xrGetInstanceProcAddr);
    }
    return result;
}
