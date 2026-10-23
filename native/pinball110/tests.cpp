#define PFDMR_PINBALL_TEST
#include "pinball.cpp"
#include <cstdio>
#include <cstdlib>
#include <cmath>
static int checks=0;
static void check(bool ok,const char* name){if(!ok){printf("FAIL %s\n",name);exit(1);}++checks;printf("PASS %s\n",name);}
static bool near(float a,float b){return fabsf(a-b)<0.00001f;}
static int mode=0;
static XrCompositionLayerColorScaleBiasKHR* sourceColor;
static XrBaseInStructure tail{XR_TYPE_UNKNOWN,nullptr};
static XrCompositionLayerProjectionView expectedView{XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW};
static XrCompositionLayerSpaceWarpInfoFB warp{XR_TYPE_COMPOSITION_LAYER_SPACE_WARP_INFO_FB};
static void frameCheck(const XrFrameEndInfo* f){
 bool submitted=false;for(uint32_t i=0;i<f->layerCount;i++)submitted|=f->layers[i]->type==XR_TYPE_COMPOSITION_LAYER_PASSTHROUGH_FB;check(pinball::camera.load()==submitted,"camera state published before downstream EndFrame");
 check(f->layerCount==20,"all twenty layers retained");
 auto p=reinterpret_cast<const XrCompositionLayerProjection*>(f->layers[19]);
 check(p->viewCount==1&&p->views==&expectedView&&p->views[0].next==&warp,"projection views and SpaceWarp extension retained verbatim");
 if(mode==0){check(!(p->layerFlags&(XR_COMPOSITION_LAYER_BLEND_TEXTURE_SOURCE_ALPHA_BIT|XR_COMPOSITION_LAYER_UNPREMULTIPLIED_ALPHA_BIT)),"VR opaque");check(p->next==sourceColor,"VR extension chain unchanged");}
 else if(mode==1){check((p->layerFlags&XR_COMPOSITION_LAYER_BLEND_TEXTURE_SOURCE_ALPHA_BIT)!=0,"MR blend retained");auto layout=(const XrCompositionLayerImageLayoutFB*)p->next;auto c=(const XrCompositionLayerColorScaleBiasKHR*)layout->next;check(layout->type==XR_TYPE_COMPOSITION_LAYER_IMAGE_LAYOUT_FB,"preceding layout retained");check(near(c->colorScale.a,-0.3f)&&near(c->colorBias.a,0.8f),"MR alpha inverted");check(c->colorScale.r==2&&c->colorBias.g==3&&c->next==&tail,"RGB and trailing extensions retained");}
 else check(p->next==&tail,"unknown extension preserved");
}
static unsigned char tickSeen=255;static float deltaSeen=0;
static void tickMock(void* p,float d){tickSeen=((unsigned char*)p)[0xa8];deltaSeen=d;}
static int ratioCalls=0;static float ratioMock(void*){++ratioCalls;return 0.75f;}
static int replacement(){return 99;}
int main(){
 auto result=(XrResult*)sym("mockResult");auto actionId=(uint64_t*)sym("mockAction");auto captured=(XrActionSpaceCreateInfo*)sym("mockSpace");auto callback=(void(**)(const XrFrameEndInfo*))sym("mockFrame");check(result&&actionId&&captured&&callback,"mock loaded");
 XrInstance instance{};XrInstanceCreateInfo ici{XR_TYPE_INSTANCE_CREATE_INFO};check(xrCreateInstance(&ici,&instance)==XR_SUCCESS,"instance direct path");
 PFN_xrVoidFunction proc{};check(xrGetInstanceProcAddr(instance,"xrCreateSession",&proc)==XR_SUCCESS&&proc==(PFN_xrVoidFunction)xrCreateSession,"resolver wraps session");XrSession session{};XrSessionCreateInfo sci{XR_TYPE_SESSION_CREATE_INFO};((PFN_xrCreateSession)proc)(instance,&sci,&session);
 check(xrGetInstanceProcAddr(instance,"notAnExtension",&proc)==XR_ERROR_FUNCTION_UNSUPPORTED&&!proc,"unknown resolver preserved");
 XrCompositionLayerColorScaleBiasKHR color{XR_TYPE_COMPOSITION_LAYER_COLOR_SCALE_BIAS_KHR};color.next=&tail;color.colorScale={2,4,5,0.3f};color.colorBias={1,3,4,0.2f};sourceColor=&color;auto oldColor=color;
 XrCompositionLayerProjection projection{XR_TYPE_COMPOSITION_LAYER_PROJECTION};projection.next=&color;projection.layerFlags=XR_COMPOSITION_LAYER_BLEND_TEXTURE_SOURCE_ALPHA_BIT|XR_COMPOSITION_LAYER_UNPREMULTIPLIED_ALPHA_BIT;
 expectedView.next=&warp;projection.viewCount=1;projection.views=&expectedView;
 XrCompositionLayerPassthroughFB pass{XR_TYPE_COMPOSITION_LAYER_PASSTHROUGH_FB};std::vector<const XrCompositionLayerBaseHeader*> layers(20,(const XrCompositionLayerBaseHeader*)&projection);XrFrameEndInfo frame{XR_TYPE_FRAME_END_INFO};frame.layerCount=20;frame.layers=layers.data();*callback=frameCheck;
 xrEndFrame(session,&frame);check(projection.layerFlags==(XR_COMPOSITION_LAYER_BLEND_TEXTURE_SOURCE_ALPHA_BIT|XR_COMPOSITION_LAYER_UNPREMULTIPLIED_ALPHA_BIT),"original layer untouched");
 XrCompositionLayerImageLayoutFB layout{XR_TYPE_COMPOSITION_LAYER_IMAGE_LAYOUT_FB};layout.next=&color;projection.next=&layout;layers[0]=(const XrCompositionLayerBaseHeader*)&pass;mode=1;xrGetInstanceProcAddr(instance,"xrEndFrame",&proc);check(proc==(PFN_xrVoidFunction)xrEndFrame,"resolver wraps frame");((PFN_xrEndFrame)proc)(session,&frame);check(!memcmp(&color,&oldColor,sizeof(color))&&layout.next==&color,"caller chain memory untouched");check(pinball::camera.load(),"MR camera set");
 mode=2;projection.next=&tail;xrEndFrame(session,&frame);*callback=nullptr;xrEndSession(session);check(!pinball::camera.load(),"end session resets camera");xrEndFrame(session,&frame);xrDestroySession(session);check(!pinball::camera.load(),"destroy session resets camera");xrCreateSession(instance,&sci,&session);
 *result=XR_ERROR_RUNTIME_FAILURE;check(xrEndFrame(session,&frame)==XR_ERROR_RUNTIME_FAILURE&&!pinball::camera.load(),"failed MR frame preserves VR camera state and error");*result=XR_SUCCESS;xrEndFrame(session,&frame);layers[0]=(const XrCompositionLayerBaseHeader*)&projection;*result=XR_ERROR_RUNTIME_FAILURE;check(xrEndFrame(session,&frame)==XR_ERROR_RUNTIME_FAILURE&&pinball::camera.load(),"failed VR frame preserves MR camera state");*result=XR_SUCCESS;layers[0]=(const XrCompositionLayerBaseHeader*)&pass;
 XrActionSet set{};XrActionSetCreateInfo asi{XR_TYPE_ACTION_SET_CREATE_INFO};xrCreateActionSet(instance,&asi,&set);XrAction action{};XrActionCreateInfo ai{XR_TYPE_ACTION_CREATE_INFO};ai.actionType=XR_ACTION_TYPE_POSE_INPUT;xrCreateAction(set,&ai,&action);
 XrActionSuggestedBinding binding{action,11};XrInteractionProfileSuggestedBinding bindings{XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING};bindings.interactionProfile=10;bindings.countSuggestedBindings=1;bindings.suggestedBindings=&binding;
 XrActionSpaceCreateInfo spaceInfo{XR_TYPE_ACTION_SPACE_CREATE_INFO};spaceInfo.action=action;spaceInfo.poseInActionSpace.orientation.w=1;spaceInfo.poseInActionSpace.position={1,2,3};XrSpace space{};
 *result=XR_ERROR_VALIDATION_FAILURE;xrSuggestInteractionProfileBindings(instance,&bindings);*result=XR_SUCCESS;xrCreateActionSpace(session,&spaceInfo,&space);check(captured->poseInActionSpace.orientation.w==1,"failed binding not registered");
 xrSuggestInteractionProfileBindings(instance,&bindings);xrGetInstanceProcAddr(instance,"xrCreateActionSpace",&proc);((PFN_xrCreateActionSpace)proc)(session,&spaceInfo,&space);check(near(captured->poseInActionSpace.orientation.x,0.70710678f)&&near(captured->poseInActionSpace.orientation.w,0.70710678f),"registered grip rotated via resolver");check(captured->poseInActionSpace.position.z==3&&spaceInfo.poseInActionSpace.orientation.x==0,"position and caller pose unchanged");
 spaceInfo.poseInActionSpace.orientation={0,0.70710678f,0,0.70710678f};xrCreateActionSpace(session,&spaceInfo,&space);auto rotated=captured->poseInActionSpace.orientation;check(near(rotated.x,0.5f)&&near(rotated.y,0.5f)&&near(rotated.z,-0.5f)&&near(rotated.w,0.5f),"nonidentity grip applies local right-multiplied X rotation");spaceInfo.poseInActionSpace.orientation={0,0,0,1};
 binding.binding=12;xrSuggestInteractionProfileBindings(instance,&bindings);xrCreateActionSpace(session,&spaceInfo,&space);check(captured->poseInActionSpace.orientation.w==1,"same profile head rebinding removes previous grip");
 binding.binding=11;bindings.interactionProfile=13;xrSuggestInteractionProfileBindings(instance,&bindings);xrCreateActionSpace(session,&spaceInfo,&space);check(captured->poseInActionSpace.orientation.w==1,"non-touch profile grip does not rotate");bindings.interactionProfile=10;xrSuggestInteractionProfileBindings(instance,&bindings);
 std::vector<XrActionSuggestedBinding> manyBindings;for(int n=0;n<12;n++){*actionId=100+n;XrAction pose{};xrCreateAction(set,&ai,&pose);manyBindings.push_back({pose,11});}auto many=bindings;many.countSuggestedBindings=manyBindings.size();many.suggestedBindings=manyBindings.data();xrSuggestInteractionProfileBindings(instance,&many);bool allRotated=true;for(const auto& b:manyBindings){spaceInfo.action=b.action;xrCreateActionSpace(session,&spaceInfo,&space);allRotated&=near(captured->poseInActionSpace.orientation.x,0.70710678f);}check(allRotated,"all twelve pose actions tracked without fixed-capacity truncation");spaceInfo.action=action;xrCreateActionSpace(session,&spaceInfo,&space);check(captured->poseInActionSpace.orientation.w==1,"replacement profile binding list removes absent old action");xrSuggestInteractionProfileBindings(instance,&bindings);
 *actionId=6;XrAction head{};xrCreateAction(set,&ai,&head);spaceInfo.action=head;xrCreateActionSpace(session,&spaceInfo,&space);check(captured->poseInActionSpace.orientation.w==1,"unrelated head pose unchanged");
 xrDestroyAction(action);*actionId=4;xrCreateAction(set,&ai,&action);spaceInfo.action=action;xrCreateActionSpace(session,&spaceInfo,&space);check(captured->poseInActionSpace.orientation.w==1,"reused action has no stale grip");xrSuggestInteractionProfileBindings(instance,&bindings);xrDestroyActionSet(set);xrCreateActionSet(instance,&asi,&set);xrCreateAction(set,&ai,&action);xrCreateActionSpace(session,&spaceInfo,&space);check(captured->poseInActionSpace.orientation.w==1,"reused action set has no stale grip");
 xrSuggestInteractionProfileBindings(instance,&bindings);xrEndFrame(session,&frame);xrDestroyInstance(instance);check(!pinball::camera.load()&&pinball::actions.empty()&&pinball::sets.empty()&&pinball::sessions.empty(),"instance teardown clears ownership and camera");
 alignas(8) unsigned char self[256]{};pinball::tickOriginal.store(tickMock);self[0xa8]=7;pinball::camera.store(false);pinball::tick(self,0.5f);check(tickSeen==0&&self[0xa8]==7&&deltaSeen==0.5f,"VR tick temporarily suppresses and restores room correction");pinball::camera.store(true);pinball::tick(self,0.25f);check(tickSeen==7&&self[0xa8]==7,"MR tick retains room correction");
 pinball::unrealBase=0x1000;uintptr_t vt=pinball::unrealBase+0x881e6e0;uintptr_t task=(uintptr_t)&vt;memcpy(self+0x70,&task,sizeof(task));pinball::ratioOriginal.store(ratioMock);
 for(unsigned char state=0;state<10;state++){self[0x49]=state;float value=pinball::ratio(self);bool guarded=state!=4&&state!=6&&state!=8;check(value==(guarded?0.0f:0.75f),"DLC progress state guard");}vt++;self[0x49]=0;check(pinball::ratio(self)==0.75f,"unrelated task delegates");task=0;memcpy(self+0x70,&task,sizeof(task));check(pinball::ratio(self)==0.75f,"null task delegates");
 task=1;memcpy(self+0x70,&task,sizeof(task));for(unsigned char completed:{4,6,8}){self[0x49]=completed;check(pinball::ratio(self)==0.75f,"completed task delegates without dereferencing stale pointer");}
#ifdef __aarch64__
 size_t size=sysconf(_SC_PAGESIZE);auto code=(uint32_t*)mmap(nullptr,size,PROT_READ|PROT_WRITE|PROT_EXEC,MAP_PRIVATE|MAP_ANONYMOUS,-1,0);check(code!=MAP_FAILED,"hook test allocation");const uint32_t sig[]={0xd503201f,0x528000e0,0xd65f03c0,0xd503201f};memcpy(code,sig,sizeof(sig));__builtin___clear_cache((char*)code,(char*)(code+4));pinball::Hook h;uint32_t bad[]={0,0,0,0};check(!pinball::prepareHook(h,(uintptr_t)code,(void*)replacement,bad),"hook rejects mismatched signature");check(pinball::prepareHook(h,(uintptr_t)code,(void*)replacement,sig),"hook prepares bounded relay");check(((int(*)())h.trampoline)()==7,"trampoline preserves relocated instruction and resumes");__atomic_store_n(code,h.branch,__ATOMIC_RELEASE);__builtin___clear_cache((char*)code,(char*)(code+1));check(((int(*)())code)()==99,"atomic branch reaches replacement");munmap(h.page,h.pageSize);munmap(code,size);
#endif
 printf("ALL %d CHECKS PASSED\n",checks);return 0;
}
