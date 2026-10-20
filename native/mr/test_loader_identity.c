#include <openxr/openxr.h>
#include <dlfcn.h>
#include <stdio.h>
int main(void) {
 const char *names[]={"libopenxr_loader.so","libopenxr_loader_yvr.so"};
 for(unsigned i=0;i<2;i++) {
  void *first=dlopen(names[i],RTLD_NOW|RTLD_LOCAL);
  if(!first){fprintf(stderr,"load failed: %s\n",dlerror());return 1;}
  PFN_xrGetInstanceProcAddr gipa=(PFN_xrGetInstanceProcAddr)dlsym(first,"xrGetInstanceProcAddr");
  PFN_xrVoidFunction fn=NULL;
  if(!gipa || gipa(XR_NULL_HANDLE,"xrPollEvent",&fn)!=XR_SUCCESS || !fn)return 2;
  void *again=dlopen(names[i],RTLD_NOW|RTLD_LOCAL);
  if(!again || dlsym(again,"xrGetInstanceProcAddr")!=(void*)gipa) {
   fprintf(stderr,"FAIL: repeated dlopen bypasses MR wrapper: %s\n",names[i]);return 3;
  }
  if(gipa(XR_NULL_HANDLE,"xrGetInstanceProcAddr",&fn)!=XR_SUCCESS || fn!=(PFN_xrVoidFunction)gipa)return 4;
  printf("PASS: repeated loader lookup and resolver lookup preserve %s\n",names[i]);
 }
 return 0;
}
