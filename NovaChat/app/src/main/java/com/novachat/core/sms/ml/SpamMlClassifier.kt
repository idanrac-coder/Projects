package com.novachat.core.sms.ml

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TFLite-based spam classifier. Loads a .tflite model from assets at first use.
 * Returns a spam probability [0, 1] or -1f if the model is unavailable.
 *
 * Expected model input:  float[1][128] (token indices)
 * Expected model output: float[1][1]   (spam probability)
 */
@Singleton
class SpamMlClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var loadAttempted = false
    private val mutex = Mutex()

    val isModelAvailable: Boolean get() = interpreter != null

    val modelVersion: Int get() = MODEL_VERSION

    companion object {
        private const val TAG = "SpamMlClassifier"
        private const val MODEL_FILENAME = "spam_classifier.tflite"
        private const val VOCAB_FILENAME = "spam_vocab.txt"
        const val MODEL_VERSION = 0
    }

    suspend fun ensureLoaded() {
        if (loadAttempted) return
        mutex.withLock {
            if (loadAttempted) return
            loadAttempted = true
            withContext(Dispatchers.IO) {
                loadModel()
                loadVocabulary()
            }
        }
    }

    private fun loadModel() {
        try {
            val assetList = context.assets.list("") ?: emptyArray()
            if (MODEL_FILENAME !in assetList) {
                Log.i(TAG, "No TFLite model found in assets ($MODEL_FILENAME), ML scoring disabled")
                return
            }
            val modelBuffer = loadMappedFile(MODEL_FILENAME)
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.i(TAG, "TFLite model loaded successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load TFLite model", e)
            interpreter = null
        }
    }

    private fun loadVocabulary() {
        try {
            val assetList = context.assets.list("") ?: emptyArray()
            if (VOCAB_FILENAME !in assetList) {
                Log.i(TAG, "No vocab file found ($VOCAB_FILENAME), using hash-based tokenization")
                return
            }
            val vocabMap = mutableMapOf<String, Int>()
            context.assets.open(VOCAB_FILENAME).bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val token = line.trim()
                    if (token.isNotEmpty()) {
                        vocabMap[token] = index + 2
                    }
                }
            }
            SpamTextPreprocessor.loadVocabulary(vocabMap)
            Log.i(TAG, "Vocabulary loaded: ${vocabMap.size} tokens")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load vocabulary", e)
        }
    }

    private fun loadMappedFile(filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val input = FileInputStream(fd.fileDescriptor)
        val channel = input.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    /**
     * Run inference on the given message body.
     * Returns spam probability [0, 1] or -1f if model is unavailable.
     */
    suspend fun classify(body: String): Float {
        ensureLoaded()
        val interp = interpreter ?: return -1f
        return withContext(Dispatchers.Default) {
            try {
                val input = SpamTextPreprocessor.preprocess(body)
                val inputArray = arrayOf(input)
                val outputArray = Array(1) { FloatArray(1) }
                interp.run(inputArray, outputArray)
                outputArray[0][0].coerceIn(0f, 1f)
            } catch (e: Exception) {
                Log.w(TAG, "TFLite inference failed", e)
                -1f
            }
        }
    }
}
