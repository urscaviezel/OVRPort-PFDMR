// SPDX-License-Identifier: GPL-3.0-only
#include "input_abi.h"
#include "runtime.h"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <vector>

namespace ovp::input {
namespace {
constexpr size_t HandCount = 2;
constexpr size_t Left = 0;
constexpr size_t Right = 1;
constexpr std::array<ovrDeviceID, HandCount> DeviceIds{1, 2};
constexpr float DigitalTriggerThreshold = 0.5f;
constexpr float JoystickDeadZone = 0.1f;
constexpr XrDuration HapticDuration = 100'000'000; // Refreshed while the requested level is nonzero.

struct Actions {
    XrAction gripPose = XR_NULL_HANDLE;
    XrAction trigger = XR_NULL_HANDLE;
    XrAction squeeze = XR_NULL_HANDLE;
    XrAction thumbstick = XR_NULL_HANDLE;
    XrAction thumbstickClick = XR_NULL_HANDLE;
    XrAction primaryClick = XR_NULL_HANDLE;
    XrAction secondaryClick = XR_NULL_HANDLE;
    XrAction primaryTouch = XR_NULL_HANDLE;
    XrAction secondaryTouch = XR_NULL_HANDLE;
    XrAction triggerTouch = XR_NULL_HANDLE;
    XrAction thumbstickTouch = XR_NULL_HANDLE;
    XrAction menuClick = XR_NULL_HANDLE;
    XrAction haptic = XR_NULL_HANDLE;
};

struct Hand {
    XrSpace gripSpace = XR_NULL_HANDLE;
    bool active = false;
    float requestedHaptic = 0.0f;
    ovrInputStateTrackedRemote state{};
};

struct InputRuntime {
    XrActionSet actionSet = XR_NULL_HANDLE;
    std::array<XrPath, HandCount> handPaths{XR_NULL_PATH, XR_NULL_PATH};
    Actions actions{};
    std::array<Hand, HandCount> hands{};
    bool attached = false;
    double lastSyncSeconds = 0.0;
};

InputRuntime g;

void clearHandState(size_t hand) {
    Hand& value = g.hands[hand];
    value.active = false;
    value.state = {};
    value.state.Header.ControllerType = ovrControllerType_TrackedRemote;
    value.state.BatteryPercentRemaining = UINT8_MAX; // OpenXR exposes no controller battery in core.
}

void stopHaptic(size_t hand) {
    Runtime& s = runtime();
    if (!g.attached || s.session == XR_NULL_HANDLE || g.actions.haptic == XR_NULL_HANDLE) {
        return;
    }
    XrHapticActionInfo info{XR_TYPE_HAPTIC_ACTION_INFO};
    info.action = g.actions.haptic;
    info.subactionPath = g.handPaths[hand];
    s.xr.xrStopHapticFeedback(s.session, &info);
}

void destroyState(bool stopHaptics) {
    Runtime& s = runtime();
    for (size_t hand = 0; hand < HandCount; ++hand) {
        if (stopHaptics && g.hands[hand].requestedHaptic > 0.0f) {
            stopHaptic(hand);
        }
        if (g.hands[hand].gripSpace != XR_NULL_HANDLE && s.xr.xrDestroySpace != nullptr) {
            s.xr.xrDestroySpace(g.hands[hand].gripSpace);
        }
    }
    if (g.actionSet != XR_NULL_HANDLE && s.xr.xrDestroyActionSet != nullptr) {
        s.xr.xrDestroyActionSet(g.actionSet);
    }
    g = {};
}

bool path(const char* text, XrPath& result) {
    Runtime& s = runtime();
    return xrOk(s.xr.xrStringToPath(s.instance, text, &result), text);
}

bool createAction(const char* name, const char* localizedName, XrActionType type, XrAction& action) {
    Runtime& s = runtime();
    XrActionCreateInfo info{XR_TYPE_ACTION_CREATE_INFO};
    info.actionType = type;
    std::snprintf(info.actionName, sizeof(info.actionName), "%s", name);
    std::snprintf(info.localizedActionName, sizeof(info.localizedActionName), "%s", localizedName);
    info.countSubactionPaths = static_cast<uint32_t>(g.handPaths.size());
    info.subactionPaths = g.handPaths.data();
    return xrOk(s.xr.xrCreateAction(g.actionSet, &info, &action), name);
}

bool addBinding(std::vector<XrActionSuggestedBinding>& bindings, XrAction action,
                size_t hand, const char* component) {
    const char* handName = hand == Left ? "left" : "right";
    char text[128];
    std::snprintf(text, sizeof(text), "/user/hand/%s/%s", handName, component);
    XrPath binding = XR_NULL_PATH;
    if (!path(text, binding)) {
        return false;
    }
    bindings.push_back({action, binding});
    return true;
}

bool getPoseActive(size_t hand) {
    Runtime& s = runtime();
    XrActionStateGetInfo info{XR_TYPE_ACTION_STATE_GET_INFO};
    info.action = g.actions.gripPose;
    info.subactionPath = g.handPaths[hand];
    XrActionStatePose state{XR_TYPE_ACTION_STATE_POSE};
    if (!xrOk(s.xr.xrGetActionStatePose(s.session, &info, &state), "xrGetActionStatePose")) {
        return false;
    }
    return state.isActive == XR_TRUE;
}

bool getBoolean(XrAction action, size_t hand) {
    Runtime& s = runtime();
    XrActionStateGetInfo info{XR_TYPE_ACTION_STATE_GET_INFO};
    info.action = action;
    info.subactionPath = g.handPaths[hand];
    XrActionStateBoolean state{XR_TYPE_ACTION_STATE_BOOLEAN};
    if (!xrOk(s.xr.xrGetActionStateBoolean(s.session, &info, &state), "xrGetActionStateBoolean")) {
        return false;
    }
    return state.isActive == XR_TRUE && state.currentState == XR_TRUE;
}

float getFloat(XrAction action, size_t hand) {
    Runtime& s = runtime();
    XrActionStateGetInfo info{XR_TYPE_ACTION_STATE_GET_INFO};
    info.action = action;
    info.subactionPath = g.handPaths[hand];
    XrActionStateFloat state{XR_TYPE_ACTION_STATE_FLOAT};
    if (!xrOk(s.xr.xrGetActionStateFloat(s.session, &info, &state), "xrGetActionStateFloat") ||
        state.isActive != XR_TRUE) {
        return 0.0f;
    }
    return std::clamp(state.currentState, 0.0f, 1.0f);
}

ovrVector2f getVector2(XrAction action, size_t hand) {
    Runtime& s = runtime();
    XrActionStateGetInfo info{XR_TYPE_ACTION_STATE_GET_INFO};
    info.action = action;
    info.subactionPath = g.handPaths[hand];
    XrActionStateVector2f state{XR_TYPE_ACTION_STATE_VECTOR2F};
    if (!xrOk(s.xr.xrGetActionStateVector2f(s.session, &info, &state), "xrGetActionStateVector2f") ||
        state.isActive != XR_TRUE) {
        return {};
    }
    return {std::clamp(state.currentState.x, -1.0f, 1.0f),
            std::clamp(state.currentState.y, -1.0f, 1.0f)};
}

XrResult applyHaptic(size_t hand, float amplitude) {
    Runtime& s = runtime();
    XrHapticActionInfo info{XR_TYPE_HAPTIC_ACTION_INFO};
    info.action = g.actions.haptic;
    info.subactionPath = g.handPaths[hand];
    XrHapticVibration vibration{XR_TYPE_HAPTIC_VIBRATION};
    vibration.duration = HapticDuration;
    vibration.frequency = XR_FREQUENCY_UNSPECIFIED;
    vibration.amplitude = amplitude;
    return s.xr.xrApplyHapticFeedback(
        s.session, &info, reinterpret_cast<const XrHapticBaseHeader*>(&vibration));
}

ovrResult hapticError(XrResult result) {
    if (result == XR_ERROR_PATH_UNSUPPORTED || result == XR_ERROR_FEATURE_UNSUPPORTED ||
        result == XR_ERROR_FUNCTION_UNSUPPORTED || result == XR_ERROR_ACTION_TYPE_MISMATCH) {
        return Unsupported;
    }
    return InvalidOperation;
}

Hand* handForId(ovrDeviceID id, size_t* index = nullptr) {
    for (size_t hand = 0; hand < HandCount; ++hand) {
        if (DeviceIds[hand] == id) {
            if (index != nullptr) {
                *index = hand;
            }
            return &g.hands[hand];
        }
    }
    return nullptr;
}

bool ready() {
    return g.actionSet != XR_NULL_HANDLE && g.attached && runtime().session != XR_NULL_HANDLE;
}

uint32_t buttonCapabilities(size_t hand) {
    using namespace input_abi;
    const uint32_t face = hand == Left ? ButtonX | ButtonY : ButtonA | ButtonB;
    const uint32_t thumb = hand == Left ? ButtonLThumb : ButtonRThumb;
    const uint32_t menu = hand == Left ? ButtonEnter : 0;
    return face | thumb | menu | ButtonGripTrigger | ButtonTrigger | ButtonJoystick;
}

uint32_t touchCapabilities(size_t hand) {
    using namespace input_abi;
    const uint32_t face = hand == Left ? TouchX | TouchY : TouchA | TouchB;
    const uint32_t thumb = hand == Left ? TouchLThumb : TouchRThumb;
    return face | thumb | TouchJoystick | TouchIndexTrigger;
}

void populateState(size_t hand) {
    using namespace input_abi;
    Hand& value = g.hands[hand];
    value.state = {};
    value.state.Header.ControllerType = ovrControllerType_TrackedRemote;
    value.state.Header.TimeInSeconds = g.lastSyncSeconds;
    value.state.BatteryPercentRemaining = UINT8_MAX;
    value.state.IndexTrigger = getFloat(g.actions.trigger, hand);
    value.state.GripTrigger = getFloat(g.actions.squeeze, hand);
    value.state.JoystickNoDeadZone = getVector2(g.actions.thumbstick, hand);
    value.state.Joystick = value.state.JoystickNoDeadZone;
    const float magnitudeSquared = value.state.Joystick.x * value.state.Joystick.x +
                                   value.state.Joystick.y * value.state.Joystick.y;
    if (magnitudeSquared < JoystickDeadZone * JoystickDeadZone) {
        value.state.Joystick = {};
    }

    if (value.state.IndexTrigger >= DigitalTriggerThreshold) value.state.Buttons |= ButtonTrigger;
    if (value.state.GripTrigger >= DigitalTriggerThreshold) value.state.Buttons |= ButtonGripTrigger;
    if (getBoolean(g.actions.thumbstickClick, hand)) {
        value.state.Buttons |= ButtonJoystick | (hand == Left ? ButtonLThumb : ButtonRThumb);
    }
    if (getBoolean(g.actions.primaryClick, hand)) {
        value.state.Buttons |= hand == Left ? ButtonX : ButtonA;
    }
    if (getBoolean(g.actions.secondaryClick, hand)) {
        value.state.Buttons |= hand == Left ? ButtonY : ButtonB;
    }
    if (hand == Left && getBoolean(g.actions.menuClick, hand)) {
        value.state.Buttons |= ButtonEnter;
    }

    if (getBoolean(g.actions.primaryTouch, hand)) {
        value.state.Touches |= hand == Left ? TouchX : TouchA;
    }
    if (getBoolean(g.actions.secondaryTouch, hand)) {
        value.state.Touches |= hand == Left ? TouchY : TouchB;
    }
    if (getBoolean(g.actions.triggerTouch, hand)) value.state.Touches |= TouchIndexTrigger;
    if (getBoolean(g.actions.thumbstickTouch, hand)) {
        value.state.Touches |= TouchJoystick | (hand == Left ? TouchLThumb : TouchRThumb);
    }
}
} // namespace

bool initialize() {
    Runtime& s = runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    destroyState(true);
    if (s.instance == XR_NULL_HANDLE || s.session == XR_NULL_HANDLE) {
        return false;
    }

    if (!path("/user/hand/left", g.handPaths[Left]) ||
        !path("/user/hand/right", g.handPaths[Right])) {
        destroyState(false);
        return false;
    }

    XrActionSetCreateInfo setInfo{XR_TYPE_ACTION_SET_CREATE_INFO};
    std::snprintf(setInfo.actionSetName, sizeof(setInfo.actionSetName), "%s", "vrapi_input");
    std::snprintf(setInfo.localizedActionSetName, sizeof(setInfo.localizedActionSetName), "%s", "VrApi input");
    if (!xrOk(s.xr.xrCreateActionSet(s.instance, &setInfo, &g.actionSet), "xrCreateActionSet")) {
        destroyState(false);
        return false;
    }

    const bool actionsCreated =
        createAction("grip_pose", "Grip pose", XR_ACTION_TYPE_POSE_INPUT, g.actions.gripPose) &&
        createAction("trigger", "Index trigger", XR_ACTION_TYPE_FLOAT_INPUT, g.actions.trigger) &&
        createAction("squeeze", "Grip trigger", XR_ACTION_TYPE_FLOAT_INPUT, g.actions.squeeze) &&
        createAction("thumbstick", "Thumbstick", XR_ACTION_TYPE_VECTOR2F_INPUT, g.actions.thumbstick) &&
        createAction("thumbstick_click", "Thumbstick click", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.thumbstickClick) &&
        createAction("primary_click", "Primary face button", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.primaryClick) &&
        createAction("secondary_click", "Secondary face button", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.secondaryClick) &&
        createAction("primary_touch", "Primary face touch", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.primaryTouch) &&
        createAction("secondary_touch", "Secondary face touch", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.secondaryTouch) &&
        createAction("trigger_touch", "Index trigger touch", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.triggerTouch) &&
        createAction("thumbstick_touch", "Thumbstick touch", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.thumbstickTouch) &&
        createAction("menu_click", "Menu button", XR_ACTION_TYPE_BOOLEAN_INPUT, g.actions.menuClick) &&
        createAction("haptic", "Controller vibration", XR_ACTION_TYPE_VIBRATION_OUTPUT, g.actions.haptic);
    if (!actionsCreated) {
        destroyState(false);
        return false;
    }

    std::vector<XrActionSuggestedBinding> bindings;
    bindings.reserve(25);
    bool bindingsCreated = true;
    for (size_t hand = 0; hand < HandCount && bindingsCreated; ++hand) {
        bindingsCreated =
            addBinding(bindings, g.actions.gripPose, hand, "input/grip/pose") &&
            addBinding(bindings, g.actions.trigger, hand, "input/trigger/value") &&
            addBinding(bindings, g.actions.squeeze, hand, "input/squeeze/value") &&
            addBinding(bindings, g.actions.thumbstick, hand, "input/thumbstick") &&
            addBinding(bindings, g.actions.thumbstickClick, hand, "input/thumbstick/click") &&
            addBinding(bindings, g.actions.triggerTouch, hand, "input/trigger/touch") &&
            addBinding(bindings, g.actions.thumbstickTouch, hand, "input/thumbstick/touch") &&
            addBinding(bindings, g.actions.haptic, hand, "output/haptic");
    }
    bindingsCreated = bindingsCreated &&
        addBinding(bindings, g.actions.primaryClick, Left, "input/x/click") &&
        addBinding(bindings, g.actions.secondaryClick, Left, "input/y/click") &&
        addBinding(bindings, g.actions.primaryTouch, Left, "input/x/touch") &&
        addBinding(bindings, g.actions.secondaryTouch, Left, "input/y/touch") &&
        addBinding(bindings, g.actions.menuClick, Left, "input/menu/click") &&
        addBinding(bindings, g.actions.primaryClick, Right, "input/a/click") &&
        addBinding(bindings, g.actions.secondaryClick, Right, "input/b/click") &&
        addBinding(bindings, g.actions.primaryTouch, Right, "input/a/touch") &&
        addBinding(bindings, g.actions.secondaryTouch, Right, "input/b/touch");

    XrPath profile = XR_NULL_PATH;
    if (!bindingsCreated || !path("/interaction_profiles/oculus/touch_controller", profile)) {
        destroyState(false);
        return false;
    }
    XrInteractionProfileSuggestedBinding suggested{XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING};
    suggested.interactionProfile = profile;
    suggested.countSuggestedBindings = static_cast<uint32_t>(bindings.size());
    suggested.suggestedBindings = bindings.data();
    if (!xrOk(s.xr.xrSuggestInteractionProfileBindings(s.instance, &suggested),
              "xrSuggestInteractionProfileBindings")) {
        destroyState(false);
        return false;
    }

    for (size_t hand = 0; hand < HandCount; ++hand) {
        XrActionSpaceCreateInfo spaceInfo{XR_TYPE_ACTION_SPACE_CREATE_INFO};
        spaceInfo.action = g.actions.gripPose;
        spaceInfo.subactionPath = g.handPaths[hand];
        // VrApi expects the legacy Touch pose, not OpenXR grip. Invert MetaXR's
        // OculusPoseToGrip transform: +60 degrees about X and (0, -0.03, 0.04)m
        // in OpenXR coordinates. Both hands use the same local rigid transform.
        spaceInfo.poseInActionSpace = {
            {-0.5f, 0.0f, 0.0f, 0.8660254038f},
            {0.0f, -0.0196410162f, -0.0459807621f},
        };
        if (!xrOk(s.xr.xrCreateActionSpace(s.session, &spaceInfo, &g.hands[hand].gripSpace),
                  "xrCreateActionSpace")) {
            destroyState(false);
            return false;
        }
        clearHandState(hand);
    }

    XrSessionActionSetsAttachInfo attach{XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO};
    attach.countActionSets = 1;
    attach.actionSets = &g.actionSet;
    if (!xrOk(s.xr.xrAttachSessionActionSets(s.session, &attach), "xrAttachSessionActionSets")) {
        destroyState(false);
        return false;
    }
    g.attached = true;
    return true;
}

void shutdown() {
    Runtime& s = runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    destroyState(true);
}

void sync() {
    Runtime& s = runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    if (!ready()) {
        for (size_t hand = 0; hand < HandCount; ++hand) clearHandState(hand);
        return;
    }

    XrActiveActionSet active{g.actionSet, XR_NULL_PATH};
    XrActionsSyncInfo info{XR_TYPE_ACTIONS_SYNC_INFO};
    info.countActiveActionSets = 1;
    info.activeActionSets = &active;
    if (!xrOk(s.xr.xrSyncActions(s.session, &info), "xrSyncActions")) {
        for (size_t hand = 0; hand < HandCount; ++hand) {
            stopHaptic(hand);
            g.hands[hand].requestedHaptic = 0.0f;
            clearHandState(hand);
        }
        return;
    }

    g.lastSyncSeconds = secondsNow();
    for (size_t hand = 0; hand < HandCount; ++hand) {
        const float requestedHaptic = g.hands[hand].requestedHaptic;
        clearHandState(hand);
        g.hands[hand].requestedHaptic = requestedHaptic;
        g.hands[hand].active = getPoseActive(hand);
        if (!g.hands[hand].active) {
            stopHaptic(hand);
            g.hands[hand].requestedHaptic = 0.0f;
            continue;
        }
        populateState(hand);
        if (g.hands[hand].requestedHaptic > 0.0f) {
            const XrResult result = applyHaptic(hand, g.hands[hand].requestedHaptic);
            if (XR_FAILED(result)) {
                xrOk(result, "xrApplyHapticFeedback refresh");
                g.hands[hand].requestedHaptic = 0.0f;
            }
        }
    }
}
} // namespace ovp::input

// Input diagnostics: every 5 s, how the game uses the controller API (FramePort, e.g. BlazeRush's untracked
// controllers): call counts, the last tracking status/position per hand, and the vibration amplitudes it asked for.
namespace ovp::input::diag {
struct Counters {
    uint32_t enumerate = 0, caps = 0, state = 0, tracking = 0, trackingFail = 0, haptic = 0;
    uint32_t stateTypes = 0, capsTypes = 0, stateRefused = 0, buttons = 0;  // ControllerType bits asked for
    float stickMax[HandCount] = {};
    uint32_t lastStatus[HandCount] = {};
    float lastPos[HandCount][3] = {};
    float hapticMin = 2.0f, hapticMax = -1.0f;
    double lastTime = 0, lastLog = 0;
};
static Counters c;
static void maybeLog() {
    const double now = ovp::secondsNow();
    if (c.lastLog == 0) c.lastLog = now;
    if (now - c.lastLog < 5.0) return;
    OVP_LOG("input 5s: enumerate=%u caps=%u state=%u tracking=%u (failed %u, time %.3f vs now %.3f) "
            "L status=0x%x pos=%.2f,%.2f,%.2f R status=0x%x pos=%.2f,%.2f,%.2f haptic=%u amp %.2f..%.2f",
            c.enumerate, c.caps, c.state, c.tracking, c.trackingFail, c.lastTime, now,
            c.lastStatus[Left], c.lastPos[Left][0], c.lastPos[Left][1], c.lastPos[Left][2],
            c.lastStatus[Right], c.lastPos[Right][0], c.lastPos[Right][1], c.lastPos[Right][2],
            c.haptic, c.haptic ? c.hapticMin : 0.0f, c.haptic ? c.hapticMax : 0.0f);
    OVP_LOG("input 5s: state types asked 0x%x (refused %u), caps types 0x%x, buttons seen 0x%x, stick max L %.2f R %.2f",
            c.stateTypes, c.stateRefused, c.capsTypes, c.buttons, c.stickMax[Left], c.stickMax[Right]);
    const double last = now;
    c = {};
    c.lastLog = last;
}
} // namespace ovp::input::diag

VRAPI ovrResult vrapi_EnumerateInputDevices(ovrMobile* mobile, uint32_t index,
                                             ovrInputCapabilityHeader* capsHeader) {
    ovp::Runtime& s = ovp::runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    if (capsHeader == nullptr) return InvalidParameter;
    ++ovp::input::diag::c.enumerate;
    if (!ovp::validMobile(mobile) || !ovp::input::ready()) return NotInitialized;
    uint32_t connectedIndex = 0;
    for (size_t hand = 0; hand < ovp::input::HandCount; ++hand) {
        if (!ovp::input::g.hands[hand].active) continue;
        if (connectedIndex++ == index) {
            capsHeader->Type = ovrControllerType_TrackedRemote;
            capsHeader->DeviceID = ovp::input::DeviceIds[hand];
            return Success;
        }
    }
    return NoDevice;
}

VRAPI ovrResult vrapi_GetInputDeviceCapabilities(ovrMobile* mobile,
                                                  ovrInputCapabilityHeader* capsHeader) {
    ovp::Runtime& s = ovp::runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    if (capsHeader == nullptr) return InvalidParameter;
    if (!ovp::validMobile(mobile) || !ovp::input::ready()) return NotInitialized;
    ++ovp::input::diag::c.caps;
    ovp::input::diag::c.capsTypes |= static_cast<uint32_t>(capsHeader->Type);
    if (capsHeader->Type != ovrControllerType_TrackedRemote) return Unsupported;
    size_t hand = 0;
    ovp::input::Hand* value = ovp::input::handForId(capsHeader->DeviceID, &hand);
    if (value == nullptr) return NoDevice;
    if (!value->active) return DeviceUnavailable;

    const ovrDeviceID id = capsHeader->DeviceID;
    auto* caps = reinterpret_cast<ovrInputTrackedRemoteCapabilities*>(capsHeader);
    *caps = {};
    caps->Header.Type = ovrControllerType_TrackedRemote;
    caps->Header.DeviceID = id;
    using namespace ovp::input_abi;
    caps->ControllerCapabilities = ControllerHasOrientationTracking |
        ControllerHasPositionTracking | ControllerHasAnalogIndexTrigger |
        ControllerHasAnalogGripTrigger | ControllerHasSimpleHapticVibration |
        ControllerHasJoystick | ControllerModelOculusTouch |
        (hand == ovp::input::Left ? ControllerLeftHand : ControllerRightHand);
    caps->ButtonCapabilities = ovp::input::buttonCapabilities(hand);
    caps->TouchCapabilities = ovp::input::touchCapabilities(hand);
    return Success;
}

VRAPI ovrResult vrapi_GetCurrentInputState(ovrMobile* mobile, ovrDeviceID deviceID,
                                            ovrInputStateHeader* inputState) {
    ovp::Runtime& s = ovp::runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    if (inputState == nullptr) return InvalidParameter;
    if (!ovp::validMobile(mobile) || !ovp::input::ready()) return NotInitialized;
    ++ovp::input::diag::c.state;
    ovp::input::diag::c.stateTypes |= static_cast<uint32_t>(inputState->ControllerType);
    ovp::input::diag::maybeLog();
    if (inputState->ControllerType != ovrControllerType_TrackedRemote) {
        ++ovp::input::diag::c.stateRefused;
        return Unsupported;
    }
    ovp::input::Hand* hand = ovp::input::handForId(deviceID);
    if (hand == nullptr) return NoDevice;
    auto* state = reinterpret_cast<ovrInputStateTrackedRemote*>(inputState);
    if (!hand->active) {
        *state = {};
        state->Header.ControllerType = ovrControllerType_TrackedRemote;
        state->BatteryPercentRemaining = UINT8_MAX;
        return DeviceUnavailable;
    }
    *state = hand->state;
    {
        auto& d = ovp::input::diag::c;
        size_t i = hand == &ovp::input::g.hands[ovp::input::Left] ? ovp::input::Left : ovp::input::Right;
        d.buttons |= state->Buttons;
        d.stickMax[i] = std::max(d.stickMax[i], std::max(std::fabs(state->Joystick.x), std::fabs(state->Joystick.y)));
    }
    return Success;
}

VRAPI ovrResult vrapi_GetInputTrackingState(ovrMobile* mobile, ovrDeviceID deviceID,
                                             double absTimeInSeconds, ovrTracking* tracking) {
    ovp::Runtime& s = ovp::runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    if (tracking == nullptr || !std::isfinite(absTimeInSeconds) || absTimeInSeconds < 0.0) {
        return InvalidParameter;
    }
    *tracking = {};
    if (!ovp::validMobile(mobile) || !ovp::input::ready()) return NotInitialized;
    size_t handIndex = 0;
    ovp::input::Hand* hand = ovp::input::handForId(deviceID, &handIndex);
    if (hand == nullptr) return NoDevice;
    if (!hand->active) return DeviceUnavailable;
    const double requestedSeconds = absTimeInSeconds == 0.0 ? ovp::secondsNow() : absTimeInSeconds;
    const XrTime requestedTime = ovp::toXrTime(requestedSeconds);
    auto& d = ovp::input::diag::c;
    ++d.tracking;
    d.lastTime = requestedSeconds;
    ovp::input::diag::maybeLog();
    if (!ovp::locate(hand->gripSpace, requestedTime, tracking->HeadPose, tracking->Status)) {
        ++d.trackingFail;
        return DeviceUnavailable;
    }
    d.lastStatus[handIndex] = tracking->Status;
    d.lastPos[handIndex][0] = tracking->HeadPose.Pose.Position.x;
    d.lastPos[handIndex][1] = tracking->HeadPose.Pose.Position.y;
    d.lastPos[handIndex][2] = tracking->HeadPose.Pose.Position.z;
    return Success;
}

VRAPI ovrResult vrapi_SetHapticVibrationSimple(ovrMobile* mobile, ovrDeviceID deviceID,
                                                float intensity) {
    ovp::Runtime& s = ovp::runtime();
    std::lock_guard<std::recursive_mutex> lock(s.mutex);
    if (!std::isfinite(intensity)) return InvalidParameter;
    if (!ovp::validMobile(mobile) || !ovp::input::ready()) return NotInitialized;
    size_t handIndex = 0;
    ovp::input::Hand* hand = ovp::input::handForId(deviceID, &handIndex);
    if (hand == nullptr) return NoDevice;
    if (!hand->active) return DeviceUnavailable;

    const float amplitude = std::clamp(intensity, 0.0f, 1.0f);
    auto& d = ovp::input::diag::c;
    if (amplitude > 0.0f) {
        ++d.haptic;
        d.hapticMin = std::min(d.hapticMin, amplitude);
        d.hapticMax = std::max(d.hapticMax, amplitude);
    }
    if (amplitude == 0.0f) {
        XrHapticActionInfo info{XR_TYPE_HAPTIC_ACTION_INFO};
        info.action = ovp::input::g.actions.haptic;
        info.subactionPath = ovp::input::g.handPaths[handIndex];
        const XrResult result = s.xr.xrStopHapticFeedback(s.session, &info);
        hand->requestedHaptic = 0.0f;
        if (XR_FAILED(result)) {
            ovp::xrOk(result, "xrStopHapticFeedback");
            return ovp::input::hapticError(result);
        }
        return Success;
    }

    const XrResult result = ovp::input::applyHaptic(handIndex, amplitude);
    if (XR_FAILED(result)) {
        ovp::xrOk(result, "xrApplyHapticFeedback");
        hand->requestedHaptic = 0.0f;
        return ovp::input::hapticError(result);
    }
    hand->requestedHaptic = amplitude;
    return Success;
}
