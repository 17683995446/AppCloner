package com.appcloner.ml

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.*

class MLRecommendationModel(
    private val context: Context
) {
    companion object {
        private const val TAG = "MLRecommendationModel"
        private const val MODEL_PREFS = "ml_model_prefs"
        private const val MODEL_VERSION = "model_version"
        private const val LAST_TRAINING = "last_training"
        private const val MODEL_ACCURACY = "model_accuracy"
        
        private const val DEFAULT_LEARNING_RATE = 0.01
        private const val DEFAULT_EPOCHS = 100
        private const val FEATURE_COUNT = 10
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        MODEL_PREFS, Context.MODE_PRIVATE
    )

    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotInitialized)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _modelMetrics = MutableStateFlow<ModelMetrics?>(null)
    val modelMetrics: StateFlow<ModelMetrics?> = _modelMetrics.asStateFlow()

    private var weights = DoubleArray(FEATURE_COUNT) { 0.0 }
    private var bias = 0.0
    private var isInitialized = false

    sealed class ModelState {
        object NotInitialized : ModelState()
        object Initializing : ModelState()
        object Training : ModelState()
        data class Ready(val accuracy: Float) : ModelState()
        data class Error(val message: String) : ModelState()
    }

    data class ModelMetrics(
        val accuracy: Float,
        val precision: Float,
        val recall: Float,
        val f1Score: Float,
        val trainingTime: Long,
        val epochCount: Int,
        val featureImportance: Map<String, Float>
    )

    data class TrainingData(
        val features: List<DoubleArray>,
        val labels: List<Int>
    )

    data class PredictionInput(
        val usageFrequency: Double,
        val sessionDuration: Double,
        val timeOfDay: Double,
        val dayOfWeek: Double,
        val appCategory: Double,
        val recentActivity: Double,
        val clonePotential: Double,
        val privacyRisk: Double,
        val crossDeviceUsage: Double,
        val socialScore: Double
    )

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            _modelState.value = ModelState.Initializing
            Log.i(TAG, "Initializing ML model...")
            
            loadModel()
            
            if (!isInitialized) {
                Log.i(TAG, "Model not found, using default weights")
                initializeDefaultWeights()
            }
            
            val accuracy = prefs.getFloat(MODEL_ACCURACY, 0.8f)
            _modelState.value = ModelState.Ready(accuracy)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model: ${e.message}")
            _modelState.value = ModelState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    private fun initializeDefaultWeights() {
        weights = DoubleArray(FEATURE_COUNT) { 
            (Math.random() - 0.5) * 0.1 
        }
        bias = (Math.random() - 0.5) * 0.1
        isInitialized = true
        saveModel()
    }

    suspend fun train(trainingData: TrainingData): Result<ModelMetrics> = withContext(Dispatchers.Default) {
        try {
            _modelState.value = ModelState.Training
            Log.i(TAG, "Training model with ${trainingData.features.size} samples...")
            
            val startTime = System.currentTimeMillis()
            
            val learningRate = DEFAULT_LEARNING_RATE
            val epochs = DEFAULT_EPOCHS
            
            var bestAccuracy = 0f
            val featureGradients = DoubleArray(FEATURE_COUNT) { 0.0 }
            var biasGradient = 0.0
            
            for (epoch in 0 until epochs) {
                var totalLoss = 0.0
                var correctPredictions = 0
                
                for (i in trainingData.features.indices) {
                    val features = trainingData.features[i]
                    val label = trainingData.labels[i]
                    
                    val prediction = predictRaw(features)
                    val probability = sigmoid(prediction)
                    val error = probability - label
                    
                    totalLoss += -label * ln(probability + 1e-10) - 
                                (1 - label) * ln(1 - probability + 1e-10)
                    
                    if ((probability > 0.5 && label == 1) ||
                        (probability <= 0.5 && label == 0)) {
                        correctPredictions++
                    }
                    
                    for (j in features.indices) {
                        featureGradients[j] += error * features[j]
                    }
                    biasGradient += error
                }
                
                val avgLoss = totalLoss / trainingData.features.size
                val accuracy = correctPredictions.toFloat() / trainingData.features.size
                
                if (accuracy > bestAccuracy) {
                    bestAccuracy = accuracy
                }
                
                for (j in weights.indices) {
                    weights[j] -= learningRate * featureGradients[j] / trainingData.features.size
                }
                bias -= learningRate * biasGradient / trainingData.features.size
                
                if (epoch % 10 == 0) {
                    Log.d(TAG, "Epoch $epoch: Loss=${String.format("%.4f", avgLoss)}, " +
                              "Accuracy=${String.format("%.2f", accuracy * 100)}%")
                }
            }
            
            val trainingTime = System.currentTimeMillis() - startTime
            val metrics = calculateMetrics(trainingData, bestAccuracy, trainingTime, epochs)
            
            saveModel()
            _modelState.value = ModelState.Ready(metrics.accuracy)
            _modelMetrics.value = metrics
            
            Log.i(TAG, "Training completed: Accuracy=${String.format("%.2f", metrics.accuracy * 100)}%")
            
            Result.success(metrics)
        } catch (e: Exception) {
            Log.e(TAG, "Training failed: ${e.message}")
            _modelState.value = ModelState.Error(e.message ?: "Training failed")
            Result.failure(e)
        }
    }

    suspend fun predict(input: PredictionInput): PredictionResult = withContext(Dispatchers.Default) {
        val features = extractFeatures(input)
        val rawPrediction = predictRaw(features)
        val probability = sigmoid(rawPrediction)
        
        val confidence = when {
            probability > 0.9 -> ConfidenceLevel.VERY_HIGH
            probability > 0.7 -> ConfidenceLevel.HIGH
            probability > 0.5 -> ConfidenceLevel.MEDIUM
            probability > 0.3 -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.VERY_LOW
        }
        
        PredictionResult(
            shouldClone = probability > 0.5,
            probability = probability.toFloat(),
            confidence = confidence,
            explanation = generateExplanation(input, probability),
            recommendedAction = getRecommendedAction(probability)
        )
    }

    private fun extractFeatures(input: PredictionInput): DoubleArray {
        return doubleArrayOf(
            input.usageFrequency,
            input.sessionDuration,
            input.timeOfDay,
            input.dayOfWeek,
            input.appCategory,
            input.recentActivity,
            input.clonePotential,
            input.privacyRisk,
            input.crossDeviceUsage,
            input.socialScore
        ).normalize()
    }

    private fun DoubleArray.normalize(): DoubleArray {
        val min = this.minOrNull() ?: return this
        val max = this.maxOrNull() ?: return this
        val range = max - min
        return if (range > 0) {
            this.map { (it - min) / range }.toDoubleArray()
        } else {
            this
        }
    }

    private fun predictRaw(features: DoubleArray): Double {
        var sum = bias
        for (i in features.indices) {
            sum += weights[i] * features[i]
        }
        return sum
    }

    private fun sigmoid(x: Double): Double {
        return 1.0 / (1.0 + exp(-x.coerceIn(-500.0, 500.0)))
    }

    private fun calculateMetrics(
        trainingData: TrainingData,
        accuracy: Float,
        trainingTime: Long,
        epochs: Int
    ): ModelMetrics {
        var truePositives = 0
        var falsePositives = 0
        var trueNegatives = 0
        var falseNegatives = 0
        
        for (i in trainingData.features.indices) {
            val prediction = predictRaw(trainingData.features[i]) > 0.0
            val actual = trainingData.labels[i] == 1
            
            when {
                prediction && actual -> truePositives++
                prediction && !actual -> falsePositives++
                !prediction && !actual -> trueNegatives++
                !prediction && actual -> falseNegatives++
            }
        }
        
        val precision = if (truePositives + falsePositives > 0) {
            truePositives.toFloat() / (truePositives + falsePositives)
        } else 0f
        
        val recall = if (truePositives + falseNegatives > 0) {
            truePositives.toFloat() / (truePositives + falseNegatives)
        } else 0f
        
        val f1Score = if (precision + recall > 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0f
        
        val featureImportance = mapFeatureImportance()
        
        return ModelMetrics(
            accuracy = accuracy,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            trainingTime = trainingTime,
            epochCount = epochs,
            featureImportance = featureImportance
        )
    }

    private fun mapFeatureImportance(): Map<String, Float> {
        val totalWeight = weights.sumOf { abs(it) }
        return mapOf(
            "usage_frequency" to (abs(weights[0]) / totalWeight * 100).toFloat(),
            "session_duration" to (abs(weights[1]) / totalWeight * 100).toFloat(),
            "time_of_day" to (abs(weights[2]) / totalWeight * 100).toFloat(),
            "day_of_week" to (abs(weights[3]) / totalWeight * 100).toFloat(),
            "app_category" to (abs(weights[4]) / totalWeight * 100).toFloat(),
            "recent_activity" to (abs(weights[5]) / totalWeight * 100).toFloat(),
            "clone_potential" to (abs(weights[6]) / totalWeight * 100).toFloat(),
            "privacy_risk" to (abs(weights[7]) / totalWeight * 100).toFloat(),
            "cross_device_usage" to (abs(weights[8]) / totalWeight * 100).toFloat(),
            "social_score" to (abs(weights[9]) / totalWeight * 100).toFloat()
        )
    }

    private fun generateExplanation(input: PredictionInput, probability: Double): String {
        val reasons = mutableListOf<String>()
        
        if (input.usageFrequency > 0.7) {
            reasons.add("高频使用应用 (${(input.usageFrequency * 100).toInt()}%)")
        }
        
        if (input.sessionDuration > 0.5) {
            reasons.add("长会话时长")
        }
        
        if (input.clonePotential > 0.6) {
            reasons.add("克隆潜力高")
        }
        
        if (input.privacyRisk > 0.5) {
            reasons.add("隐私风险需要注意")
        }
        
        if (input.crossDeviceUsage > 0.5) {
            reasons.add("跨设备使用需求")
        }
        
        return if (reasons.isNotEmpty()) {
            "基于 ${reasons.take(3).joinToString("、")}，建议${if (probability > 0.5) "克隆" else "不克隆"}"
        } else {
            "综合分析建议${if (probability > 0.5) "克隆" else "不克隆"}此应用"
        }
    }

    private fun getRecommendedAction(probability: Double): RecommendedAction {
        return when {
            probability > 0.9 -> RecommendedAction.STRONGLY_RECOMMEND
            probability > 0.7 -> RecommendedAction.RECOMMEND
            probability > 0.5 -> RecommendedAction.CONSIDER
            probability > 0.3 -> RecommendedAction.NOT_RECOMMEND
            else -> RecommendedAction.STRONGLY_NOT_RECOMMEND
        }
    }

    private fun saveModel() {
        prefs.edit()
            .putInt(MODEL_VERSION, 1)
            .putLong(LAST_TRAINING, System.currentTimeMillis())
            .putFloat(MODEL_ACCURACY, _modelMetrics.value?.accuracy ?: 0.8f)
            .apply()
        
        val weightsJson = JSONArray()
        weights.forEach { weightsJson.put(it) }
        prefs.edit().putString("model_weights", weightsJson.toString()).apply()
        prefs.edit().putFloat("model_bias", bias.toFloat()).apply()
    }

    private fun loadModel(): Boolean {
        val version = prefs.getInt(MODEL_VERSION, 0)
        if (version == 0) {
            return false
        }
        
        return try {
            val weightsJson = prefs.getString("model_weights", null)
            if (weightsJson != null) {
                val jsonArray = JSONArray(weightsJson)
                weights = DoubleArray(jsonArray.length()) { 
                    jsonArray.getDouble(it) 
                }
                bias = prefs.getFloat("model_bias", 0f).toDouble()
                isInitialized = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}")
            false
        }
    }

    fun generateSyntheticTrainingData(): TrainingData {
        val features = mutableListOf<DoubleArray>()
        val labels = mutableListOf<Int>()
        
        repeat(1000) {
            val usageFreq = Math.random()
            val sessionDur = Math.random()
            val timeOfDay = Math.random()
            val dayOfWeek = Math.random()
            val category = Math.random()
            val recentAct = Math.random()
            val clonePot = Math.random()
            val privacyRisk = Math.random()
            val crossDevice = Math.random()
            val socialScore = Math.random()
            
            val shouldClone = usageFreq > 0.6 && clonePot > 0.5 ||
                             (privacyRisk > 0.7 && crossDevice > 0.4)
            
            features.add(doubleArrayOf(
                usageFreq, sessionDur, timeOfDay, dayOfWeek, category,
                recentAct, clonePot, privacyRisk, crossDevice, socialScore
            ))
            labels.add(if (shouldClone) 1 else 0)
        }
        
        return TrainingData(features, labels)
    }

    enum class ConfidenceLevel {
        VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW
    }

    data class PredictionResult(
        val shouldClone: Boolean,
        val probability: Float,
        val confidence: ConfidenceLevel,
        val explanation: String,
        val recommendedAction: RecommendedAction
    )

    enum class RecommendedAction {
        STRONGLY_RECOMMEND,
        RECOMMEND,
        CONSIDER,
        NOT_RECOMMEND,
        STRONGLY_NOT_RECOMMEND
    }

    fun exportModel(): String {
        return JSONObject().apply {
            put("version", 1)
            put("weights", JSONArray(weights.toList()))
            put("bias", bias)
            put("accuracy", _modelMetrics.value?.accuracy ?: 0.8f)
            put("exportedAt", System.currentTimeMillis())
        }.toString()
    }

    fun importModel(modelJson: String): Boolean {
        return try {
            val json = JSONObject(modelJson)
            val weightsArray = json.getJSONArray("weights")
            weights = DoubleArray(weightsArray.length()) {
                weightsArray.getDouble(it)
            }
            bias = json.getDouble("bias")
            isInitialized = true
            saveModel()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import model: ${e.message}")
            false
        }
    }
}
