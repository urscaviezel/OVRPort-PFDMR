#include <openxr/openxr.h>
#include <dlfcn.h>
#include <cstdio>
#include <cstdlib>
#include <cmath>
#include "parallel_math.h"
static int checks = 0;
void check(bool ok, const char* name) { if (!ok) { std::printf("FAIL %s\n", name); std::exit(1); } std::printf("PASS %s\n", name); checks++; }
int main() {
    void* runtime = dlopen("libpfdmr_original_openxr.so", RTLD_NOW|RTLD_LOCAL);
    check(runtime, "mock runtime loads");
    auto mode = reinterpret_cast<void(*)(int)>(dlsym(runtime,"mockMode"));
    void* wrapper = dlopen("libopenxr_loader.so", RTLD_NOW|RTLD_LOCAL);
    if (!wrapper) std::printf("%s\n", dlerror());
    check(wrapper, "production ARM64 stereo library loads on PFDMR");
    auto locate = reinterpret_cast<PFN_xrLocateViews>(dlsym(wrapper,"xrLocateViews"));
    auto get = reinterpret_cast<PFN_xrGetInstanceProcAddr>(dlsym(wrapper,"xrGetInstanceProcAddr"));
    XrViewLocateInfo info{XR_TYPE_VIEW_LOCATE_INFO}; info.viewConfigurationType = XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;
    XrViewState state{XR_TYPE_VIEW_STATE}; XrView views[2] = {{XR_TYPE_VIEW},{XR_TYPE_VIEW}}; uint32_t count=0;
    auto r = locate(XR_NULL_HANDLE,&info,&state,2,&count,views);
    check(r == XR_SUCCESS && count == 2, "direct xrLocateViews succeeds");
    check(std::abs(views[0].pose.orientation.y)<1e-6f && std::abs(views[1].pose.orientation.y)<1e-6f && std::abs(views[0].pose.orientation.w-1)<1e-6f, "8 degree eye cant becomes parallel");
    check(views[0].pose.position.x == -0.0315f && views[1].pose.position.x == 0.0315f && views[0].pose.position.y == 1.5f, "eye positions and IPD preserved");
    check(views[0].fov.angleLeft == -0.82f && views[1].fov.angleUp == 0.78f, "FOV preserved");
    PFN_xrVoidFunction fn = nullptr;
    check(get(XR_NULL_HANDLE,"xrLocateViews",&fn)==XR_SUCCESS && fn == reinterpret_cast<PFN_xrVoidFunction>(locate), "xrGetInstanceProcAddr intercepts xrLocateViews");
    mode(4); reinterpret_cast<PFN_xrLocateViews>(fn)(XR_NULL_HANDLE,&info,&state,2,&count,views);
    check(std::abs(views[0].pose.orientation.y)<1e-6f && views[0].pose.orientation.w>0.99999f, "opposite quaternion signs handled");
    mode(1); locate(XR_NULL_HANDLE,&info,&state,2,&count,views);
    check(views[0].pose.orientation.y > 0.03f && views[1].pose.orientation.y < -0.03f, "invalid orientation flags leave poses unchanged");
    mode(0); info.viewConfigurationType=XR_VIEW_CONFIGURATION_TYPE_PRIMARY_MONO;
    locate(XR_NULL_HANDLE,&info,&state,2,&count,views);
    check(views[0].pose.orientation.y>0.03f, "non-stereo configuration unchanged");
    info.viewConfigurationType=XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO;
    check(locate(XR_NULL_HANDLE,&info,&state,0,&count,nullptr)==XR_SUCCESS && count==2, "view count query works");
    check(locate(XR_NULL_HANDLE,&info,&state,1,&count,views)==XR_ERROR_SIZE_INSUFFICIENT, "insufficient capacity error preserved");
    mode(2); locate(XR_NULL_HANDLE,&info,&state,2,&count,views);
    check(count==1 && views[0].pose.orientation.y>0.03f, "single returned view unchanged");
    mode(3); check(locate(XR_NULL_HANDLE,&info,&state,2,&count,views)==XR_ERROR_RUNTIME_FAILURE, "runtime error preserved");
    check(get(XR_NULL_HANDLE,"xrEndFrame",&fn)==XR_SUCCESS && reinterpret_cast<PFN_xrEndFrame>(fn)(XR_NULL_HANDLE,nullptr)==XR_SUCCESS, "other functions forwarded through proc lookup");
    auto end=reinterpret_cast<PFN_xrEndFrame>(dlsym(wrapper,"xrEndFrame"));
    check(end(XR_NULL_HANDLE,nullptr)==XR_SUCCESS, "direct xrEndFrame forwarded");
    check(get(XR_NULL_HANDLE,"xrUnknown",&fn)==XR_ERROR_FUNCTION_UNSUPPORTED && fn==nullptr, "unsupported function error preserved");
    XrQuaternionf zero{0,0,0,0}, out{}, nan{NAN,0,0,1};
    check(!parallelMidpoint(zero,zero,out) && !parallelMidpoint(nan,nan,out), "degenerate quaternion midpoint rejected");
    std::printf("ALL %d NATIVE CHECKS PASSED\n", checks);
}
