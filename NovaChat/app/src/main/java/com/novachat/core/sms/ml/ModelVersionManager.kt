package com.novachat.core.sms.ml

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the bundled TFLite model version and provides upgrade-readiness checks.
 * Model metadata is stored in assets/spam_model_meta.json:
 * { "version": 1, "min_app_version": 110, "languages": ["en","he"], "created": "2026-03-28" }
 *
 * When Dynamic Asset Delivery is integrated, this class will compare local vs remote versions.
 */
@Singleton
class ModelVersionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ModelVersionManager"
        private const val META_FILENAME = "spam_model_meta.json"
    }

    data class ModelMeta(
        val version: Int,
        val minAppVersion: Int,
        val languages: List<String>,
        val created: String
    )

    var currentMeta: ModelMeta? = null
        private set

    suspend fun loadMetadata(): ModelMeta? = withContext(Dispatchers.IO) {
        try {
            val assetList = context.assets.list("") ?: emptyArray()
            if (META_FILENAME !in assetList) {
                Log.i(TAG, "No model metadata found")
                return@withContext null
            }
            val json = context.assets.open(META_FILENAME).bufferedReader().readText()
            val obj = JSONObject(json)
            val meta = ModelMeta(
                version = obj.optInt("version", 0),
                minAppVersion = obj.optInt("min_app_version", 0),
                languages = buildList {
                    val arr = obj.optJSONArray("languages")
                    if (arr != null) {
                        for (i in 0 until arr.length()) add(arr.getString(i))
                    }
                },
                created = obj.optString("created", "")
            )
            currentMeta = meta
            Log.i(TAG, "Model meta loaded: v${meta.version}, langs=${meta.languages}")
            meta
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load model metadata", e)
            null
        }
    }

    fun isModelOutdated(remoteVersion: Int): Boolean {
        val local = currentMeta?.version ?: return true
        return remoteVersion > local
    }
}
