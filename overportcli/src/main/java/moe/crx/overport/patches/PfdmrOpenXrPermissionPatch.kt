// PFDMR compatibility addition, 2026-10-10. GPL-3.0.
package moe.crx.overport.patches

import com.reandroid.json.JSONArray
import com.reandroid.json.JSONObject
import moe.crx.overport.patching.Patch
import moe.crx.overport.utils.*

val PATCH_PFDMR_OPENXR_PERMISSION = Patch("patch_pfdmr_openxr_permission", false, false) {
    selectManifestJson {
        takeNodesEach({ named("manifest") }) {
            takeNodes {
                val result = JSONArray()
                var inserted = false
                for (i in 0 until (this?.length() ?: 0)) {
                    val node = this?.opt(i)
                    if (node is JSONObject && node.named("uses-permission") &&
                        node.nameAttribute() == "org.khronos.openxr.permission.OPENXR_SYSTEM") continue
                    if (!inserted && node is JSONObject && node.named("application")) {
                        result.put(createUsesPermission("org.khronos.openxr.permission.OPENXR_SYSTEM"))
                        inserted = true
                    }
                    result.put(node)
                }
                if (!inserted) result.put(createUsesPermission("org.khronos.openxr.permission.OPENXR_SYSTEM"))
                result
            }
        }
    }
}
