#include <openxr/openxr.h>
#include <dlfcn.h>
#include <android/log.h>
#include <atomic>
#include <cstring>
#include "parallel_math.h"

extern "C" __attribute__((used, visibility("default"))) const char pfdmrStereoMarker[] = "PFDMR_PARALLEL_STEREO_V1";
void* sym(const char* name) {
    static void* library = [] {
        void* p = dlopen("libpfdmr_original_openxr.so", RTLD_NOW | RTLD_LOCAL);
        __android_log_print(p ? ANDROID_LOG_INFO : ANDROID_LOG_ERROR, "PFDMRStereo",
            "Stereo-Fix 0.1.0-beta.1: parallel eyes; original loader %s", p ? "loaded" : dlerror());
        return p;
    }();
    return library ? dlsym(library, name) : nullptr;
}
static std::atomic<PFN_xrLocateViews> originalLocate{nullptr};
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrLocateViews(XrSession session, const XrViewLocateInfo* info,
    XrViewState* state, uint32_t capacity, uint32_t* count, XrView* views) {
    auto locate = originalLocate.load();
    if (!locate) locate = reinterpret_cast<PFN_xrLocateViews>(sym("xrLocateViews"));
    if (!locate) return XR_ERROR_FUNCTION_UNSUPPORTED;
    XrResult result = locate(session, info, state, capacity, count, views);
    if (XR_SUCCEEDED(result) && info && info->viewConfigurationType == XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
        && state && count && views && capacity >= 2 && *count == 2
        && (state->viewStateFlags & XR_VIEW_STATE_ORIENTATION_VALID_BIT)) {
        XrQuaternionf midpoint;
        if (parallelMidpoint(views[0].pose.orientation, views[1].pose.orientation, midpoint)) {
            views[0].pose.orientation = midpoint;
            views[1].pose.orientation = midpoint;
        }
    }
    return result;
}
extern "C" XRAPI_ATTR XrResult XRAPI_CALL xrGetInstanceProcAddr(XrInstance instance, const char* name, PFN_xrVoidFunction* fn) {
    auto get = reinterpret_cast<PFN_xrGetInstanceProcAddr>(sym("xrGetInstanceProcAddr"));
    if (!get) return XR_ERROR_INITIALIZATION_FAILED;
    XrResult result = get(instance, name, fn);
    if (XR_SUCCEEDED(result) && name && fn && *fn && !strcmp(name, "xrLocateViews")) {
        originalLocate.store(reinterpret_cast<PFN_xrLocateViews>(*fn));
        *fn = reinterpret_cast<PFN_xrVoidFunction>(xrLocateViews);
    }
    return result;
}
