// SPDX-License-Identifier: GPL-3.0-only
#pragma once
#include "abi.h"

// Original declarations of the public, 64-bit VrApi tracked-remote ABI.
// These declarations preserve the calling convention without incorporating SDK code.
using ovrDeviceID = uint32_t;

enum ovrControllerType : int32_t {
    ovrControllerType_None = 0,
    ovrControllerType_TrackedRemote = 1 << 2,
};

struct ovrInputCapabilityHeader {
    ovrControllerType Type;
    ovrDeviceID DeviceID;
};

struct ovrInputTrackedRemoteCapabilities {
    ovrInputCapabilityHeader Header;
    uint32_t ControllerCapabilities;
    uint32_t ButtonCapabilities;
    uint16_t TrackpadMaxX;
    uint16_t TrackpadMaxY;
    float TrackpadSizeX;
    float TrackpadSizeY;
    uint32_t HapticSamplesMax;
    uint32_t HapticSampleDurationMS;
    uint32_t TouchCapabilities;
    uint32_t Reserved4;
    uint32_t Reserved5;
};

struct ovrInputStateHeader {
    ovrControllerType ControllerType;
    double TimeInSeconds;
};

struct ovrInputStateTrackedRemote {
    ovrInputStateHeader Header;
    uint32_t Buttons;
    uint32_t TrackpadStatus;
    ovrVector2f TrackpadPosition;
    uint8_t BatteryPercentRemaining;
    uint8_t RecenterCount;
    uint16_t Reserved;
    float IndexTrigger;
    float GripTrigger;
    uint32_t Touches;
    uint32_t Reserved5a;
    ovrVector2f Joystick;
    ovrVector2f JoystickNoDeadZone;
};

namespace ovp::input_abi {
constexpr uint32_t ButtonA = 0x00000001;
constexpr uint32_t ButtonB = 0x00000002;
constexpr uint32_t ButtonRThumb = 0x00000004;
constexpr uint32_t ButtonX = 0x00000100;
constexpr uint32_t ButtonY = 0x00000200;
constexpr uint32_t ButtonLThumb = 0x00000400;
constexpr uint32_t ButtonEnter = 0x00100000;
constexpr uint32_t ButtonGripTrigger = 0x04000000;
constexpr uint32_t ButtonTrigger = 0x20000000;
constexpr uint32_t ButtonJoystick = 0x80000000;

constexpr uint32_t TouchA = 0x00000001;
constexpr uint32_t TouchB = 0x00000002;
constexpr uint32_t TouchX = 0x00000004;
constexpr uint32_t TouchY = 0x00000008;
constexpr uint32_t TouchJoystick = 0x00000020;
constexpr uint32_t TouchIndexTrigger = 0x00000040;
constexpr uint32_t TouchLThumb = 0x00000400;
constexpr uint32_t TouchRThumb = 0x00000800;

constexpr uint32_t ControllerHasOrientationTracking = 0x00000001;
constexpr uint32_t ControllerHasPositionTracking = 0x00000002;
constexpr uint32_t ControllerLeftHand = 0x00000004;
constexpr uint32_t ControllerRightHand = 0x00000008;
constexpr uint32_t ControllerHasAnalogIndexTrigger = 0x00000040;
constexpr uint32_t ControllerHasAnalogGripTrigger = 0x00000080;
constexpr uint32_t ControllerHasSimpleHapticVibration = 0x00000200;
constexpr uint32_t ControllerHasJoystick = 0x00002000;
constexpr uint32_t ControllerModelOculusTouch = 0x00004000;
}

static_assert(sizeof(ovrInputCapabilityHeader) == 8);
static_assert(offsetof(ovrInputCapabilityHeader, DeviceID) == 4);
static_assert(sizeof(ovrInputTrackedRemoteCapabilities) == 48);
static_assert(offsetof(ovrInputTrackedRemoteCapabilities, ControllerCapabilities) == 8);
static_assert(offsetof(ovrInputTrackedRemoteCapabilities, TrackpadMaxX) == 16);
static_assert(offsetof(ovrInputTrackedRemoteCapabilities, HapticSamplesMax) == 28);
static_assert(offsetof(ovrInputTrackedRemoteCapabilities, TouchCapabilities) == 36);
static_assert(sizeof(ovrInputStateHeader) == 16);
static_assert(offsetof(ovrInputStateHeader, TimeInSeconds) == 8);
static_assert(sizeof(ovrInputStateTrackedRemote) == 72);
static_assert(offsetof(ovrInputStateTrackedRemote, Buttons) == 16);
static_assert(offsetof(ovrInputStateTrackedRemote, BatteryPercentRemaining) == 32);
static_assert(offsetof(ovrInputStateTrackedRemote, IndexTrigger) == 36);
static_assert(offsetof(ovrInputStateTrackedRemote, Touches) == 44);
static_assert(offsetof(ovrInputStateTrackedRemote, Joystick) == 52);
static_assert(offsetof(ovrInputStateTrackedRemote, JoystickNoDeadZone) == 60);
