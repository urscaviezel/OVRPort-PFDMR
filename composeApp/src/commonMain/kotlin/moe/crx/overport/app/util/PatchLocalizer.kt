package moe.crx.overport.app.util

import androidx.compose.runtime.Composable
import moe.crx.overport.patches.*
import moe.crx.overport.patching.Patch
import org.jetbrains.compose.resources.stringResource
import overportapp.composeapp.generated.resources.*

object PatchLocalizer {
    val LOCALIZED_PATCHES = mapOf(
        PATCH_COPY_LIBRARIES to Res.string.patch_copy_libraries,
        PATCH_COPY_OVRPLUGIN_VRAPI to Res.string.patch_copy_ovrplugin_vrapi,
        PATCH_REPLACE_ICON_LABEL to Res.string.patch_replace_icon_label,
        PATCH_FIX_MIN_ANDROID_SDK to Res.string.patch_fix_min_android_sdk,
        PATCH_GENERATE_CONFIG to Res.string.patch_generate_config,
        PATCH_REMOVE_LOCALIZED_NAMES to Res.string.patch_remove_localized_names,
        PATCH_CLEAN_UP_FRIDA to Res.string.patch_clean_up_frida,
        PATCH_OCULUS_UNITY to Res.string.patch_oculus_unity,
        PATCH_OCULUS_UNREAL to Res.string.patch_oculus_unreal,
        PATCH_VR_METADATA to Res.string.patch_vr_metadata,
        PATCH_LAUNCHER_ENTRY to Res.string.patch_launcher_entry,
        PATCH_REMOVE_USES_LIBRARY to Res.string.patch_remove_uses_library,
        PATCH_FIX_UNREAL_CRASH to Res.string.patch_fix_unreal_crash,
        PATCH_META_XR_AUDIO to Res.string.patch_meta_xr_audio,
        PATCH_MARK_AS_DEBUGGABLE to Res.string.patch_mark_as_debuggable,
        PATCH_MARK_ALLOW_BACKUP to Res.string.patch_mark_allow_backup,
        PATCH_REMOVE_VRAPI to Res.string.patch_remove_vrapi,
        PATCH_REMOVE_UNREAL_FORCE_QUIT to Res.string.patch_remove_unreal_force_quit,
        PATCH_FORCE_PASSTHROUGH to Res.string.patch_force_passthrough,
        PATCH_DISABLE_SPACE_WARP to Res.string.patch_disable_space_warp,
        PATCH_DISABLE_CONTROLLER_OFFSET to Res.string.patch_disable_controller_offset,
    )

    @Composable
    fun localizedPatch(patch: Patch): String {
        if (patch.name == "patch_pfdmr_openxr_permission") return "PFDMR OpenXR compatibility (minimum)"
        if (patch.name == moe.crx.overport.pfdmr.StereoPatch.ID) return moe.crx.overport.pfdmr.StereoPatch.LABEL
        if (patch.name == moe.crx.overport.pfdmr.UnrealAudioPatch.ID) return moe.crx.overport.pfdmr.UnrealAudioPatch.LABEL
        if (patch.name == moe.crx.overport.pfdmr.BasicPassthroughPatch.ID) return moe.crx.overport.pfdmr.BasicPassthroughPatch.LABEL
        if (patch.name == moe.crx.overport.pfdmr.RefreshRatePatch.ID) return moe.crx.overport.pfdmr.RefreshRatePatch.LABEL
        if (patch.name == moe.crx.overport.pfdmr.MrCompatibilityPatch.ID) return moe.crx.overport.pfdmr.MrCompatibilityPatch.LABEL
        if (patch.name == moe.crx.overport.pfdmr.QuestGatePatch.ID) return moe.crx.overport.pfdmr.QuestGatePatch.LABEL
        if (patch.name == moe.crx.overport.pfdmr.VulkanHeapPatch.ID) return moe.crx.overport.pfdmr.VulkanHeapPatch.LABEL
        if (patch.name == moe.crx.overport.pfdmr.UnrealObbPatch.ID) return moe.crx.overport.pfdmr.UnrealObbPatch.LABEL
        return LOCALIZED_PATCHES[patch]?.let {
            stringResource(it)
        } ?: patch.name
    }
}
