#include <openxr/openxr.h>
#include <cstring>
#include <cmath>
static int mode = 0;
extern "C" void mockMode(int value) { mode = value; }
extern "C" XrResult xrLocateViews(XrSession, const XrViewLocateInfo*, XrViewState* state, uint32_t cap, uint32_t* count, XrView* views) {
    if (mode == 3) return XR_ERROR_RUNTIME_FAILURE;
    *count = mode == 2 ? 1 : 2;
    state->viewStateFlags = mode == 1 ? 0 : XR_VIEW_STATE_ORIENTATION_VALID_BIT;
    if (cap == 0) return XR_SUCCESS;
    if (cap < *count) return XR_ERROR_SIZE_INSUFFICIENT;
    for (uint32_t i = 0; i < *count; i++) {
        const float angle = (i == 0 ? 1 : -1) * 0.034906585f;
        views[i].pose.orientation = {0, std::sin(angle), 0, std::cos(angle)};
        views[i].pose.position = {i == 0 ? -0.0315f : 0.0315f, 1.5f, -2.0f};
        views[i].fov = {-0.82f, 0.82f, 0.78f, -0.78f};
    }
    if (mode == 4) { auto& q = views[1].pose.orientation; q.x=-q.x; q.y=-q.y; q.z=-q.z; q.w=-q.w; }
    return XR_SUCCESS;
}
extern "C" XrResult xrEndFrame(XrSession, const XrFrameEndInfo*) { return XR_SUCCESS; }
extern "C" XrResult xrGetInstanceProcAddr(XrInstance, const char* name, PFN_xrVoidFunction* fn) {
    if (!strcmp(name,"xrLocateViews")) *fn = reinterpret_cast<PFN_xrVoidFunction>(xrLocateViews);
    else if (!strcmp(name,"xrEndFrame")) *fn = reinterpret_cast<PFN_xrVoidFunction>(xrEndFrame);
    else { *fn = nullptr; return XR_ERROR_FUNCTION_UNSUPPORTED; }
    return XR_SUCCESS;
}
