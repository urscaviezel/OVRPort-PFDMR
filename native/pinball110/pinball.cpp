// Pinball FX VR 1.10 / build 7380 only. GPL-3.0.
#include <openxr/openxr.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstring>
#include <memory>
#include <mutex>
#include <unordered_map>
#include <unordered_set>
#include <vector>
#include "hooks.h"
extern "C" __attribute__((used,visibility("default"))) const char pfdmrPinballMarker[]="PFDMR_PINBALL110_V1";
void* sym(const char* name){
    static void* library=dlopen("libpfdmr_pinball110_original.so",RTLD_NOW|RTLD_LOCAL);
    return library?dlsym(library,name):nullptr;
}
template<class T> static T original(const char* name){return reinterpret_cast<T>(sym(name));}
namespace pinball {
struct Action {XrActionSet set;XrActionType type;};
struct Session {XrInstance instance;bool camera=false;};
struct Instance {std::unordered_map<XrPath,std::unordered_set<XrAction>> grips;};
static std::mutex stateMutex;
static std::unordered_map<XrInstance,Instance> instances;
static std::unordered_map<XrSession,Session> sessions;
static std::unordered_map<XrActionSet,XrInstance> sets;
static std::unordered_map<XrAction,Action> actions;
static void updateCamera(){bool any=false;for(const auto& p:sessions)any|=p.second.camera;camera.store(any,std::memory_order_release);}
static void eraseAction(XrAction action){actions.erase(action);for(auto& i:instances)for(auto& p:i.second.grips)p.second.erase(action);}
static void eraseSet(XrActionSet set){
    for(auto it=actions.begin();it!=actions.end();)if(it->second.set==set){auto action=it->first;++it;eraseAction(action);}else ++it;
    sets.erase(set);
}
static bool grip(XrSession session,XrAction action){
    std::lock_guard<std::mutex> lock(stateMutex);
    auto s=sessions.find(session);auto a=actions.find(action);
    if(s==sessions.end()||a==actions.end()||a->second.type!=XR_ACTION_TYPE_POSE_INPUT)return false;
    auto owner=sets.find(a->second.set);if(owner==sets.end()||owner->second!=s->second.instance)return false;
    auto i=instances.find(s->second.instance);if(i==instances.end())return false;
    for(const auto& p:i->second.grips)if(p.second.count(action))return true;
    return false;
}
static XrQuaternionf rotateGrip(XrQuaternionf q){constexpr float h=0.7071067811865475f;return {h*(q.x+q.w),h*(q.y+q.z),h*(q.z-q.y),h*(q.w-q.x)};}
struct LayerCopy {
    XrCompositionLayerProjection projection{};
    XrCompositionLayerColorScaleBiasKHR color{};
    std::vector<std::unique_ptr<XrCompositionLayerImageLayoutFB>> layouts;
    bool invert(const void* next){
        auto n=static_cast<const XrBaseInStructure*>(next);
        // Only known nodes preceding ColorScaleBias can be safely copied. Unknown
        // nodes retain their complete original chain rather than guessed sizes.
        for(int depth=0;n&&depth<64;depth++,n=n->next){
            if(n->type==XR_TYPE_COMPOSITION_LAYER_IMAGE_LAYOUT_FB){
                layouts.push_back(std::make_unique<XrCompositionLayerImageLayoutFB>(*reinterpret_cast<const XrCompositionLayerImageLayoutFB*>(n)));
            }else if(n->type==XR_TYPE_COMPOSITION_LAYER_COLOR_SCALE_BIAS_KHR){
                color=*reinterpret_cast<const XrCompositionLayerColorScaleBiasKHR*>(n);
                color.colorScale.a=-color.colorScale.a;color.colorBias.a=1.0f-color.colorBias.a;
                void* head=&color;
                for(auto it=layouts.rbegin();it!=layouts.rend();++it){(*it)->next=head;head=it->get();}
                projection.next=head;return true;
            }else return false;
        }return false;
    }
};
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateInstance(const XrInstanceCreateInfo* info,XrInstance* instance){
    auto fn=original<PFN_xrCreateInstance>("xrCreateInstance");if(!fn)return XR_ERROR_FUNCTION_UNSUPPORTED;
    if(!pinball::installHooks()){
        __android_log_print(ANDROID_LOG_ERROR,"PFDMRPinball","Verified Unreal hooks unavailable; refusing partial profile");
        return XR_ERROR_INITIALIZATION_FAILED;
    }
    XrResult r=fn(info,instance);
    if(XR_SUCCEEDED(r)&&instance){std::lock_guard<std::mutex> lock(pinball::stateMutex);pinball::instances[*instance]={};}
    return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroyInstance(XrInstance instance){
    auto fn=original<PFN_xrDestroyInstance>("xrDestroyInstance");XrResult r=fn?fn(instance):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)){
        std::lock_guard<std::mutex> lock(pinball::stateMutex);
        for(auto it=pinball::sets.begin();it!=pinball::sets.end();)if(it->second==instance){auto set=it->first;++it;pinball::eraseSet(set);}else ++it;
        for(auto it=pinball::sessions.begin();it!=pinball::sessions.end();)if(it->second.instance==instance)it=pinball::sessions.erase(it);else ++it;
        pinball::instances.erase(instance);pinball::updateCamera();
    }return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateSession(XrInstance instance,const XrSessionCreateInfo* info,XrSession* session){
    auto fn=original<PFN_xrCreateSession>("xrCreateSession");XrResult r=fn?fn(instance,info,session):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)&&session){std::lock_guard<std::mutex> lock(pinball::stateMutex);pinball::sessions[*session]={instance,false};pinball::updateCamera();}return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroySession(XrSession session){
    auto fn=original<PFN_xrDestroySession>("xrDestroySession");XrResult r=fn?fn(session):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)){std::lock_guard<std::mutex> lock(pinball::stateMutex);pinball::sessions.erase(session);pinball::updateCamera();}return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEndSession(XrSession session){
    auto fn=original<PFN_xrEndSession>("xrEndSession");XrResult r=fn?fn(session):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)){std::lock_guard<std::mutex> lock(pinball::stateMutex);auto it=pinball::sessions.find(session);if(it!=pinball::sessions.end())it->second.camera=false;pinball::updateCamera();}return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateActionSet(XrInstance instance,const XrActionSetCreateInfo* info,XrActionSet* set){
    auto fn=original<PFN_xrCreateActionSet>("xrCreateActionSet");XrResult r=fn?fn(instance,info,set):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)&&set){std::lock_guard<std::mutex> lock(pinball::stateMutex);pinball::sets[*set]=instance;}return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroyActionSet(XrActionSet set){
    auto fn=original<PFN_xrDestroyActionSet>("xrDestroyActionSet");XrResult r=fn?fn(set):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)){std::lock_guard<std::mutex> lock(pinball::stateMutex);pinball::eraseSet(set);}return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateAction(XrActionSet set,const XrActionCreateInfo* info,XrAction* action){
    auto fn=original<PFN_xrCreateAction>("xrCreateAction");XrResult r=fn?fn(set,info,action):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)&&info&&action){std::lock_guard<std::mutex> lock(pinball::stateMutex);pinball::eraseAction(*action);pinball::actions[*action]={set,info->actionType};}return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrDestroyAction(XrAction action){
    auto fn=original<PFN_xrDestroyAction>("xrDestroyAction");XrResult r=fn?fn(action):XR_ERROR_FUNCTION_UNSUPPORTED;
    if(XR_SUCCEEDED(r)){std::lock_guard<std::mutex> lock(pinball::stateMutex);pinball::eraseAction(action);}return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrSuggestInteractionProfileBindings(XrInstance instance,const XrInteractionProfileSuggestedBinding* info){
    auto fn=original<PFN_xrSuggestInteractionProfileBindings>("xrSuggestInteractionProfileBindings");if(!fn)return XR_ERROR_FUNCTION_UNSUPPORTED;
    XrResult r=fn(instance,info);if(XR_FAILED(r)||!info)return r;
    auto path=original<PFN_xrPathToString>("xrPathToString");if(!path)return r;
    char name[XR_MAX_PATH_LENGTH]{};uint32_t count=0;
    if(XR_FAILED(path(instance,info->interactionProfile,sizeof(name),&count,name)))return r;
    bool touch=!strcmp(name,"/interaction_profiles/oculus/touch_controller")
        ||!strcmp(name,"/interaction_profiles/facebook/touch_controller_pro")
        ||!strcmp(name,"/interaction_profiles/meta/touch_controller_plus");
    std::unordered_set<XrAction> grips;
    if(touch)for(uint32_t i=0;i<info->countSuggestedBindings;i++){
        if(XR_SUCCEEDED(path(instance,info->suggestedBindings[i].binding,sizeof(name),&count,name))
            &&(!strcmp(name,"/user/hand/left/input/grip/pose")||!strcmp(name,"/user/hand/right/input/grip/pose")))grips.insert(info->suggestedBindings[i].action);
    }
    std::lock_guard<std::mutex> lock(pinball::stateMutex);auto owner=pinball::instances.find(instance);
    if(owner!=pinball::instances.end())owner->second.grips[info->interactionProfile]=std::move(grips);
    return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrCreateActionSpace(XrSession session,const XrActionSpaceCreateInfo* info,XrSpace* space){
    auto fn=original<PFN_xrCreateActionSpace>("xrCreateActionSpace");if(!fn)return XR_ERROR_FUNCTION_UNSUPPORTED;
    if(!info||!pinball::grip(session,info->action))return fn(session,info,space);
    auto copy=*info;copy.poseInActionSpace.orientation=pinball::rotateGrip(copy.poseInActionSpace.orientation);
    return fn(session,&copy,space);
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrEndFrame(XrSession session,const XrFrameEndInfo* info){
    auto fn=original<PFN_xrEndFrame>("xrEndFrame");if(!fn)return XR_ERROR_FUNCTION_UNSUPPORTED;
    if(!info||(info->layerCount&&!info->layers))return fn(session,info);
    bool passthrough=false;for(uint32_t i=0;i<info->layerCount;i++)if(info->layers[i]&&info->layers[i]->type==XR_TYPE_COMPOSITION_LAYER_PASSTHROUGH_FB)passthrough=true;
    std::vector<const XrCompositionLayerBaseHeader*> layers(info->layerCount);
    std::vector<pinball::LayerCopy> copies(info->layerCount);
    for(uint32_t i=0;i<info->layerCount;i++){
        layers[i]=info->layers[i];if(!layers[i]||layers[i]->type!=XR_TYPE_COMPOSITION_LAYER_PROJECTION)continue;
        auto& copy=copies[i];copy.projection=*reinterpret_cast<const XrCompositionLayerProjection*>(layers[i]);
        if(!passthrough)copy.projection.layerFlags&=~(XR_COMPOSITION_LAYER_BLEND_TEXTURE_SOURCE_ALPHA_BIT|XR_COMPOSITION_LAYER_UNPREMULTIPLIED_ALPHA_BIT);
        else if(!copy.invert(copy.projection.next)){
            static std::atomic<bool> warned{false};if(!warned.exchange(true))__android_log_print(ANDROID_LOG_WARN,"PFDMRPinball","Unrecognized projection alpha chain preserved");
        }
        layers[i]=reinterpret_cast<const XrCompositionLayerBaseHeader*>(&copy.projection);
    }
    bool previous=false,tracked=false;
    {std::lock_guard<std::mutex> lock(pinball::stateMutex);auto it=pinball::sessions.find(session);
        if(it!=pinball::sessions.end()){tracked=true;previous=it->second.camera;it->second.camera=passthrough;}pinball::updateCamera();}
    // Publish before forwarding: Unreal's game thread may tick during EndFrame.
    // Failed submissions restore this session's prior state.
    auto frame=*info;frame.layers=layers.data();XrResult r=fn(session,&frame);
    if(XR_FAILED(r)&&tracked){std::lock_guard<std::mutex> lock(pinball::stateMutex);auto it=pinball::sessions.find(session);if(it!=pinball::sessions.end())it->second.camera=previous;pinball::updateCamera();}
    return r;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrLocateViews(XrSession s,const XrViewLocateInfo* i,XrViewState* st,uint32_t c,uint32_t* n,XrView* v){auto fn=original<PFN_xrLocateViews>("xrLocateViews");return fn?fn(s,i,st,c,n,v):XR_ERROR_FUNCTION_UNSUPPORTED;}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProcAddr(XrInstance instance,const char* name,PFN_xrVoidFunction* out){
    auto fn=original<PFN_xrGetInstanceProcAddr>("xrGetInstanceProcAddr");if(!fn)return XR_ERROR_INITIALIZATION_FAILED;
    XrResult r=fn(instance,name,out);if(XR_FAILED(r)||!name||!out||!*out)return r;
#define WRAP(n) if(!strcmp(name,#n))*out=reinterpret_cast<PFN_xrVoidFunction>(n)
    WRAP(xrGetInstanceProcAddr);WRAP(xrCreateInstance);WRAP(xrDestroyInstance);
    WRAP(xrCreateSession);WRAP(xrDestroySession);WRAP(xrEndSession);WRAP(xrEndFrame);
    WRAP(xrCreateActionSet);WRAP(xrDestroyActionSet);WRAP(xrCreateAction);WRAP(xrDestroyAction);
    WRAP(xrSuggestInteractionProfileBindings);WRAP(xrCreateActionSpace);
#undef WRAP
    return r;
}
