package moe.crx.overport.patches

import com.reandroid.json.JSONArray
import com.reandroid.json.JSONObject
import moe.crx.overport.patching.Patch
import moe.crx.overport.utils.*

fun createAction(value: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "action").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", value)
        )
    )
}

fun createCategory(value: String): JSONObject {
    return JSONObject().put("node_type", "element").put("name", "category").put(
        "attributes", JSONArray().put(
            JSONObject().put("name", "name").put("id", 16842755)
                .put("uri", "http://schemas.android.com/apk/res/android").put("prefix", "android")
                .put("value_type", "STRING").put("data", value)
        )
    )
}

/** Preserve legacy MAIN/INFO and expose the same game activity to VR and Android launchers. */
fun ensureLegacyVrEntry(manifest: JSONObject?) {
    manifest.takeNodesEach({ named("manifest") }) {
        takeNodesEach({ named("application") }) {
            takeNodesEach({ named("activity") }) {
                val filters = elem<JSONArray>("nodes").elemEach<JSONObject> { named("intent-filter") }
                fun JSONObject.hasEntry(category: String): Boolean {
                    val entries = elem<JSONArray>("nodes").elemEach<JSONObject>()
                    return entries.any { it.named("action") && it.nameAttribute() == "android.intent.action.MAIN" } &&
                        entries.any { it.named("category") && it.nameAttribute() == category }
                }
                if (filters.any { it.hasEntry("android.intent.category.INFO") }) {
                    val hasLauncher = filters.any { it.hasEntry("android.intent.category.LAUNCHER") }
                    val vrFilter = filters.firstOrNull { it.hasEntry("com.yvr.intent.category.VR") }
                    if (vrFilter == null) {
                        val entries = JSONArray().put(createAction("android.intent.action.MAIN"))
                            .put(createCategory("com.yvr.intent.category.VR"))
                        if (!hasLauncher) entries.put(createCategory("android.intent.category.LAUNCHER"))
                        takeNodes {
                            (this ?: JSONArray()).put(JSONObject().put("node_type", "element")
                                .put("name", "intent-filter").put("nodes", entries))
                        }
                    } else if (!hasLauncher) {
                        vrFilter.getJSONArray("nodes").put(createCategory("android.intent.category.LAUNCHER"))
                    }
                }
                this
            }
        }
    }
}

val PATCH_LAUNCHER_ENTRY = Patch("patch_launcher_entry") {
    selectManifestJson {
        ensureLegacyVrEntry(this)
        takeNodesEach({ named("manifest") }) {
            takeNodesEach({ named("application") }) {
                takeNodesEach({ named("activity") }) {
                    takeNodesEach({ named("intent-filter") }) {
                        val hasIntent = elem<JSONArray>("nodes").elemEach<JSONObject> { named("category") }.map {
                            when (it.nameAttribute()) {
                                "com.oculus.intent.category.VR" -> true
                                "android.intent.category.LAUNCHER" -> true
                                else -> false
                            }
                        }.reduceOrNull { l, r -> l || r } ?: false

                        takeNodes {
                            if (!hasIntent) this else {
                                JSONArray().put(createAction("android.intent.action.MAIN"))
                                    .put(createCategory("android.intent.category.LAUNCHER"))
                                    .put(createCategory("com.oculus.intent.category.VR"))
                                    .put(createCategory("com.yvr.intent.category.VR"))
                                    .put(createCategory("org.khronos.openxr.intent.category.IMMERSIVE_HMD"))
                            }
                        }
                    }
                }
            }
        }
    }
}