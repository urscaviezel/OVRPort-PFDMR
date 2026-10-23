#include <jni.h>
#include <cstring>
static int mockHz=72;static bool mockException=false,mockFail=false;
static JNIEnv fakeEnv;static JavaVM fakeVm;
static JNINativeInterface envTable{};static JNIInvokeInterface vmTable{};
static JavaVM* mockJavaVm(){
    envTable.NewGlobalRef=[](JNIEnv*,jobject o)->jobject{return o;};
    envTable.ExceptionCheck=[](JNIEnv*)->jboolean{return mockException;};
    envTable.ExceptionClear=[](JNIEnv*){mockException=false;};
    envTable.PushLocalFrame=[](JNIEnv*,jint)->jint{return JNI_OK;};
    envTable.PopLocalFrame=[](JNIEnv*,jobject o)->jobject{return o;};
    envTable.GetObjectClass=[](JNIEnv*,jobject)->jclass{return reinterpret_cast<jclass>(1);};
    envTable.GetMethodID=[](JNIEnv*,jclass,const char* name,const char* sig)->jmethodID{
        if(strcmp(name,"getContentResolver")||strcmp(sig,"()Landroid/content/ContentResolver;"))abort();return reinterpret_cast<jmethodID>(1);
    };
    envTable.CallObjectMethodV=[](JNIEnv*,jobject,jmethodID,va_list)->jobject{return reinterpret_cast<jobject>(1);};
    envTable.FindClass=[](JNIEnv*,const char* name)->jclass{if(strcmp(name,"android/provider/Settings$System"))abort();return reinterpret_cast<jclass>(1);};
    envTable.GetStaticMethodID=[](JNIEnv*,jclass,const char* name,const char* sig)->jmethodID{
        if(strcmp(name,"getInt")||strcmp(sig,"(Landroid/content/ContentResolver;Ljava/lang/String;I)I"))abort();return reinterpret_cast<jmethodID>(1);
    };
    envTable.NewStringUTF=[](JNIEnv*,const char* name)->jstring{if(strcmp(name,"SpatialHome_SystemFrequencyMode"))abort();return reinterpret_cast<jstring>(1);};
    envTable.CallStaticIntMethodV=[](JNIEnv*,jclass,jmethodID,va_list)->jint{mockException=mockFail;return mockHz;};
    fakeEnv.functions=&envTable;
    vmTable.GetEnv=[](JavaVM*,void** env,jint)->jint{*env=&fakeEnv;return JNI_OK;};
    fakeVm.functions=&vmTable;return &fakeVm;
}
