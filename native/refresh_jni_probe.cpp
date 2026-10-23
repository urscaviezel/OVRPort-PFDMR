#include <jni.h>
#include <openxr/openxr.h>
#include <dlfcn.h>
#include <string>
extern "C" JNIEXPORT jfloat JNICALL Java_RefreshJniProbe_request(JNIEnv* env,jclass,jobject context,jstring directory){
    const char* dir=env->GetStringUTFChars(directory,nullptr);std::string base(dir);env->ReleaseStringUTFChars(directory,dir);
    auto wrapper=dlopen((base+"/libopenxr_loader.so").c_str(),RTLD_NOW|RTLD_LOCAL);if(!wrapper)return -1;
    auto create=(PFN_xrCreateInstance)dlsym(wrapper,"xrCreateInstance");
    auto rate=(PFN_xrRequestDisplayRefreshRateFB)dlsym(wrapper,"xrRequestDisplayRefreshRateFB");
    JavaVM* vm;env->GetJavaVM(&vm);
    struct AndroidInfo{XrStructureType type;const void* next;void* vm;void* context;};
    AndroidInfo android{XR_TYPE_INSTANCE_CREATE_INFO_ANDROID_KHR,nullptr,vm,context};
    XrInstanceCreateInfo info{XR_TYPE_INSTANCE_CREATE_INFO};info.next=&android;XrInstance instance;
    if(!create||!rate||create(&info,&instance)!=XR_SUCCESS)return -2;
    if(rate((XrSession)1,90)!=XR_SESSION_LOSS_PENDING)return -3;
    auto original=dlopen("libpfdmr_refresh_original.so",RTLD_NOW|RTLD_LOCAL);
    auto recorded=(float(*)())dlsym(original,"recordedRate");return recorded?recorded():-4;
}
