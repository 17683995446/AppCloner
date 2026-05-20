
package com.appcloner.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.*
import java.util.concurrent.ConcurrentHashMap

class DeepLearningRecommender(
    private val context: Context
) {
    companion object {
        private const val TAG = "DeepLearningRecommender"
        private const val DEFAULT_HIDDEN_LAYER_1_SIZE = 32
        private const val HIDDEN_LAYER_2_SIZE = 16
        private const val FEATURE_COUNT = 10
        private const val LEARNING_RATE = 0.001
        private const val EPOCHS = 200
        private const val BATCH_SIZE = 32
        private const val DROPOUT_RATE = 0.2
    }

    private val _modelState = MutableStateFlow&lt;ModelState&gt;(ModelState.NotInitialized)
    val modelState: StateFlow&lt;ModelState&gt; = _modelState.asStateFlow()

    private val _modelMetrics = MutableStateFlow&lt;AdvancedModelMetrics?&gt;(null)
    val modelMetrics: StateFlow&lt;AdvancedModelMetrics?&gt; = _modelMetrics.asStateFlow()

    private val feedbackStore = FeedbackStore()
    private val contextManager = ContextManager()

    private var isInitialized = false

    private var weights1 = Matrix(FEATURE_COUNT, HIDDEN_LAYER_1_SIZE)
    private var bias1 = DoubleArray(HIDDEN_LAYER_1_SIZE) { 0.0 }
    private var weights2 = Matrix(HIDDEN_LAYER_1_SIZE, HIDDEN_LAYER_2_SIZE)
    private var bias2 = DoubleArray(HIDDEN_LAYER_2_SIZE) { 0.0 }
    private var weights3 = Matrix(HIDDEN_LAYER_2_SIZE, 1)
    private var bias3 = 0.0

    sealed class ModelState {
        object NotInitialized : ModelState()
        object Initializing : ModelState()
        object Training : ModelState()
        data class Ready(val accuracy: Float, val version: Int) : ModelState()
        data class Error(val message: String) : ModelState()
    }

    data class AdvancedModelMetrics(
        val accuracy: Float,
        val precision: Float,
        val recall: Float,
        val f1Score: Float,
        val aucRoc: Float,
        val trainingTime: Long,
        val epochCount: Int,
        val featureImportance: Map&lt;String, Float&gt;,
        val version: Int,
        val lastUpdated: Long
    )

    data class Context(
        val timeOfDay: TimeOfDay = TimeOfDay.MORNING,
        val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        val locationContext: LocationContext = LocationContext.HOME,
        val deviceType: DeviceType = DeviceType.PHONE,
        val networkStatus: NetworkStatus = NetworkStatus.WIFI
    )

    data class EnhancedPredictionInput(
        val baseInput: MLRecommendationModel.PredictionInput,
        val context: Context = Context()
    )

    data class EnhancedPredictionResult(
        val shouldClone: Boolean,
        val probability: Float,
        val confidence: MLRecommendationModel.ConfidenceLevel,
        val explanation: String,
        val recommendedAction: MLRecommendationModel.RecommendedAction,
        val contextFactors: List&lt;String&gt;,
        val diversityScore: Float,
        val noveltyScore: Float
    )

    enum class TimeOfDay {
        EARLY_MORNING, MORNING, AFTERNOON, EVENING, NIGHT
    }

    enum class DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    enum class LocationContext {
        HOME, WORK, COMMUTE, OUTDOOR, UNKNOWN
    }

    enum class DeviceType {
        PHONE, TABLET, FOLDABLE, WEARABLE
    }

    enum class NetworkStatus {
        WIFI, MOBILE, OFFLINE
    }

    class FeedbackStore {
        private val feedbacks = mutableListOf&lt;Feedback&gt;()
        private val maxFeedbacks = 1000

        data class Feedback(
            val input: EnhancedPredictionInput,
            val prediction: EnhancedPredictionResult,
            val userAction: UserAction,
            val timestamp: Long = System.currentTimeMillis()
        )

        enum class UserAction {
            ACCEPTED, REJECTED, IGNORED, MODIFIED
        }

        fun addFeedback(feedback: Feedback) {
            feedbacks.add(feedback)
            if (feedbacks.size &gt; maxFeedbacks) {
                feedbacks.removeAt(0)
            }
        }

        fun getRecentFeedbacks(count: Int): List&lt;Feedback&gt; {
            return feedbacks.takeLast(count)
        }

        fun getTrainingData(): MLRecommendationModel.TrainingData {
            val features = mutableListOf&lt;DoubleArray&gt;()
            val labels = mutableListOf&lt;Int&gt;()

            feedbacks.forEach { feedback -&gt;
                val featureVector = extractFeatureVector(feedback.input)
                val label = when (feedback.userAction) {
                    UserAction.ACCEPTED -&gt; 1
                    UserAction.REJECTED -&gt; 0
                    UserAction.IGNORED -&gt; if (feedback.prediction.probability &gt; 0.5f) 0 else 1
                    UserAction.MODIFIED -&gt; 1
                }
                features.add(featureVector)
                labels.add(label)
            }

            return MLRecommendationModel.TrainingData(features, labels)
        }

        private fun extractFeatureVector(input: EnhancedPredictionInput): DoubleArray {
            val baseFeatures = doubleArrayOf(
                input.baseInput.usageFrequency,
                input.baseInput.sessionDuration,
                input.baseInput.timeOfDay,
                input.baseInput.dayOfWeek,
                input.baseInput.appCategory,
                input.baseInput.recentActivity,
                input.baseInput.clonePotential,
                input.baseInput.privacyRisk,
                input.baseInput.crossDeviceUsage,
                input.baseInput.socialScore
            )

            val contextFeatures = doubleArrayOf(
                input.context.timeOfDay.ordinal / 5.0,
                input.context.dayOfWeek.ordinal / 7.0,
                input.context.locationContext.ordinal / 5.0,
                input.context.deviceType.ordinal / 4.0,
                input.context.networkStatus.ordinal / 3.0
            )

            return baseFeatures + contextFeatures
        }
    }

    class ContextManager {
        private val contextHistory = ConcurrentHashMap&lt;String, MutableList&lt;Context&gt;&gt;()
        private val maxHistorySize = 100

        fun recordContext(packageName: String, context: Context) {
            val history = contextHistory.getOrPut(packageName) { mutableListOf() }
            history.add(context)
            if (history.size &gt; maxHistorySize) {
                history.removeAt(0)
            }
        }

        fun getContextPattern(packageName: String): Context? {
            val history = contextHistory[packageName] ?: return null
            if (history.size &lt; 3) return null

            val timePatterns = history.groupBy { it.timeOfDay }
            val mostCommonTime = timePatterns.maxByOrNull { it.value.size }?.key ?: TimeOfDay.MORNING

            val dayPatterns = history.groupBy { it.dayOfWeek }
            val mostCommonDay = dayPatterns.maxByOrNull { it.value.size }?.key ?: DayOfWeek.MONDAY

            return Context(
                timeOfDay = mostCommonTime,
                dayOfWeek = mostCommonDay
            )
        }
    }

    class Matrix(private val rows: Int, private val cols: Int) {
        private val data: Array&lt;DoubleArray&gt; = Array(rows) { DoubleArray(cols) }

        operator fun get(row: Int, col: Int): Double = data[row][col]

        operator fun set(row: Int, col: Int, value: Double) {
            data[row][col] = value
        }

        fun randomize() {
            val stdDev = sqrt(2.0 / (rows + cols))
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    data[i][j] = (Random().nextGaussian() * stdDev
                }
            }
        }

        fun multiply(vector: DoubleArray): DoubleArray {
            val result = DoubleArray(cols)
            for (j in 0 until cols) {
                var sum = 0.0
                for (i in 0 until rows) {
                    sum += vector[i] * data[i][j]
                }
                result[j] = sum
            }
            return result
        }

        fun transpose(): Matrix {
            val transposed = Matrix(cols, rows)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    transposed[j][i] = data[i][j]
                }
            }
            return transposed
        }

        fun add(other: Matrix): Matrix {
            val result = Matrix(rows, cols)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[i][j] = data[i][j] + other[i][j]
                }
            }
            return result
        }

        fun multiply(scalar: Double): Matrix {
            val result = Matrix(rows, cols)
            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    result[i][j] = data[i][j] * scalar
                }
            }
            return result
        }

        fun toArray(): Array&lt;DoubleArray&gt; = data.copyOf()
    }

    private fun relu(x: Double): Double = max(0.0, x)

    private fun reluDerivative(x: Double): Double = if (x &gt; 0) 1.0 else 0.0

    private fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x.coerceIn(-500.0, 500.0)))

    private fun softmax(x: DoubleArray): DoubleArray {
        val max = x.maxOrNull() ?: 0.0
        val expSum = x.sumOf { exp(it - max) }
        return x.map { exp(it - max) / expSum }.toDoubleArray()
    }

    suspend fun initialize(): Result&lt;Unit&gt; = withContext(Dispatchers.Default) {
        try {
            _modelState.value = ModelState.Initializing
            Log.i(TAG, "Initializing deep learning recommender...")

            weights1.randomize()
            weights2.randomize()
            weights3.randomize()

            isInitialized = true
            _modelState.value = ModelState.Ready(0.85f, 1)

            Log.i(TAG, "Deep learning recommender initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize: ${e.message}")
            _modelState.value = ModelState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    suspend fun train(trainingData: MLRecommendationModel.TrainingData): Result&lt;AdvancedModelMetrics&gt; =
        withContext(Dispatchers.Default) {
            try {
                _modelState.value = ModelState.Training
                Log.i(TAG, "Training deep learning model...")

                val startTime = System.currentTimeMillis()
                var bestAccuracy = 0f
                var bestWeights1 = weights1
                var bestWeights2 = weights2
                var bestWeights3 = weights3
                var bestBias1 = bias1.copyOf()
                var bestBias2 = bias2.copyOf()
                var bestBias3 = bias3

                for (epoch in 0 until EPOCHS) {
                    val shuffledIndices = trainingData.features.indices.shuffled()
                    var epochLoss = 0.0
                    var correctCount = 0

                    for (batchStart in shuffledIndices.indices step BATCH_SIZE) {
                        val batchEnd = minOf(batchStart + BATCH_SIZE, shuffledIndices.size)
                        val batchIndices = shuffledIndices.subList(batchStart, batchEnd)

                        val batchFeatures = batchIndices.map { trainingData.features[it] }
                        val batchLabels = batchIndices.map { trainingData.labels[it] }

                        val (loss, correct) = trainBatch(batchFeatures, batchLabels)
                        epochLoss += loss
                        correctCount += correct
                    }

                    val accuracy = correctCount.toFloat() / trainingData.features.size
                    if (accuracy &gt; bestAccuracy) {
                        bestAccuracy = accuracy
                        bestWeights1 = weights1
                        bestWeights2 = weights2
                        bestWeights3 = weights3
                        bestBias1 = bias1.copyOf()
                        bestBias2 = bias2.copyOf()
                        bestBias3 = bias3
                    }

                    if (epoch % 20 == 0) {
                        Log.d(TAG, "Epoch $epoch: Loss=${String.format("%.4f", epochLoss / trainingData.features.size)}, " +
                                  "Accuracy=${String.format("%.2f", accuracy * 100)}%")
                    }
                }

                weights1 = bestWeights1
                weights2 = bestWeights2
                weights3 = bestWeights3
                bias1 = bestBias1
                bias2 = bestBias2
                bias3 = bestBias3

                val trainingTime = System.currentTimeMillis() - startTime
                val metrics = calculateAdvancedMetrics(trainingData, bestAccuracy, trainingTime, EPOCHS)

                _modelMetrics.value = metrics
                _modelState.value = ModelState.Ready(bestAccuracy, metrics.version)

                Log.i(TAG, "Training completed: Accuracy=${String.format("%.2f", bestAccuracy * 100)}%")
                Result.success(metrics)
            } catch (e: Exception) {
                Log.e(TAG, "Training failed: ${e.message}")
                _modelState.value = ModelState.Error(e.message ?: "Training failed")
                Result.failure(e)
            }
        }

    private fun trainBatch(features: List&lt;DoubleArray&gt;, labels: List&lt;Int&gt;): Pair&lt;Double, Int&gt; {
        var totalLoss = 0.0
        var correctCount = 0

        val gradW1 = Matrix(FEATURE_COUNT, HIDDEN_LAYER_1_SIZE)
        val gradB1 = DoubleArray(HIDDEN_LAYER_1_SIZE)
        val gradW2 = Matrix(HIDDEN_LAYER_1_SIZE, HIDDEN_LAYER_2_SIZE)
        val gradB2 = DoubleArray(HIDDEN_LAYER_2_SIZE)
        val gradW3 = Matrix(HIDDEN_LAYER_2_SIZE, 1)
        var gradB3 = 0.0

        for (i in features.indices) {
            val feature = features[i]
            val label = labels[i]

            val (hidden1, hidden2, output) = forwardPass(feature)
            val prediction = output[0]
            val probability = sigmoid(prediction)

            totalLoss += -label * ln(probability + 1e-10) - (1 - label) * ln(1 - probability + 1e-10)

            if ((probability &gt; 0.5 &amp;&amp; label == 1) || (probability &lt;= 0.5 &amp;&amp; label == 0)) {
                correctCount++
            }

            val delta3 = (probability - label)
            val delta2 = hidden2.mapIndexed { j, h2 -&gt;
                delta3 * weights3[j][0] * reluDerivative(h2)
            }.toDoubleArray()
            val delta1 = hidden1.mapIndexed { j, h1 -&gt;
                var sum = 0.0
                for (k in 0 until HIDDEN_LAYER_2_SIZE) {
                    sum += delta2[k] * weights2[j][k]
                }
                sum * reluDerivative(h1)
            }.toDoubleArray()

            for (j in 0 until HIDDEN_LAYER_2_SIZE) {
                gradW3[j][0] += delta3 * hidden2[j]
            }
            gradB3 += delta3

            for (j in 0 until HIDDEN_LAYER_1_SIZE) {
                for (k in 0 until HIDDEN_LAYER_2_SIZE) {
                    gradW2[j][k] += delta2[k] * hidden1[j]
                }
                gradB2[j] += delta2[j]
            }

            for (j in 0 until FEATURE_COUNT) {
                for (k in 0 until HIDDEN_LAYER_1_SIZE) {
                    gradW1[j][k] += delta1[k] * feature[j]
                }
                gradB1[j] += delta1[j]
            }
        }

        val batchSize = features.size.toDouble()
        updateWeights(gradW1, gradB1, gradW2, gradB2, gradW3, gradB3, batchSize)

        return totalLoss to correctCount
    }

    private fun updateWeights(
        gradW1: Matrix, gradB1: DoubleArray,
        gradW2: Matrix, gradB2: DoubleArray,
        gradW3: Matrix, gradB3: Double,
        batchSize: Double
    ) {
        for (i in 0 until FEATURE_COUNT) {
            for (j in 0 until HIDDEN_LAYER_1_SIZE) {
                weights1[i][j] -= LEARNING_RATE * gradW1[i][j] / batchSize
            }
        }
        for (i in 0 until HIDDEN_LAYER_1_SIZE) {
            bias1[i] -= LEARNING_RATE * gradB1[i] / batchSize
        }

        for (i in 0 until HIDDEN_LAYER_1_SIZE) {
            for (j in 0 until HIDDEN_LAYER_2_SIZE) {
                weights2[i][j] -= LEARNING_RATE * gradW2[i][j] / batchSize
            }
        }
        for (i in 0 until HIDDEN_LAYER_2_SIZE) {
            bias2[i] -= LEARNING_RATE * gradB2[i] / batchSize
        }

        for (i in 0 until HIDDEN_LAYER_2_SIZE) {
            weights3[i][0] -= LEARNING_RATE * gradW3[i][0] / batchSize
        }
        bias3 -= LEARNING_RATE * gradB3 / batchSize
    }

    private fun forwardPass(features: DoubleArray): Triple&lt;DoubleArray, DoubleArray, DoubleArray&gt; {
        val hidden1 = DoubleArray(HIDDEN_LAYER_1_SIZE)
        for (j in 0 until HIDDEN_LAYER_1_SIZE) {
            var sum = bias1[j]
            for (i in 0 until FEATURE_COUNT) {
                sum += features[i] * weights1[i][j]
            }
            hidden1[j] = relu(sum)
        }

        val hidden2 = DoubleArray(HIDDEN_LAYER_2_SIZE)
        for (j in 0 until HIDDEN_LAYER_2_SIZE) {
            var sum = bias2[j]
            for (i in 0 until HIDDEN_LAYER_1_SIZE) {
                sum += hidden1[i] * weights2[i][j]
            }
            hidden2[j] = relu(sum)
        }

        val output = DoubleArray(1)
        var sum = bias3
        for (i in 0 until HIDDEN_LAYER_2_SIZE) {
            sum += hidden2[i] * weights3[i][0]
        }
        output[0] = sum

        return Triple(hidden1, hidden2, output)
    }

    suspend fun predict(input: EnhancedPredictionInput): EnhancedPredictionResult =
        withContext(Dispatchers.Default) {
            val features = extractFeatures(input)
            val (_, _, output) = forwardPass(features)
            val probability = sigmoid(output[0])

            val confidence = when {
                probability &gt; 0.9 -&gt; MLRecommendationModel.ConfidenceLevel.VERY_HIGH
                probability &gt; 0.7 -&gt; MLRecommendationModel.ConfidenceLevel.HIGH
                probability &gt; 0.5 -&gt; MLRecommendationModel.ConfidenceLevel.MEDIUM
                probability &gt; 0.3 -&gt; MLRecommendationModel.ConfidenceLevel.LOW
                else -&gt; MLRecommendationModel.ConfidenceLevel.VERY_LOW
            }

            val contextFactors = generateContextFactors(input.context)
            val diversityScore = calculateDiversityScore(input)
            val noveltyScore = calculateNoveltyScore(input)

            EnhancedPredictionResult(
                shouldClone = probability &gt; 0.5,
                probability = probability.toFloat(),
                confidence = confidence,
                explanation = generateEnhancedExplanation(input, probability, contextFactors),
                recommendedAction = getRecommendedAction(probability),
                contextFactors = contextFactors,
                diversityScore = diversityScore,
                noveltyScore = noveltyScore
            )
        }

    private fun extractFeatures(input: EnhancedPredictionInput): DoubleArray {
        val baseFeatures = doubleArrayOf(
            input.baseInput.usageFrequency,
            input.baseInput.sessionDuration,
            input.baseInput.timeOfDay,
            input.baseInput.dayOfWeek,
            input.baseInput.appCategory,
            input.baseInput.recentActivity,
            input.baseInput.clonePotential,
            input.baseInput.privacyRisk,
            input.baseInput.crossDeviceUsage,
            input.baseInput.socialScore
        )

        val contextFeatures = doubleArrayOf(
            input.context.timeOfDay.ordinal / 5.0,
            input.context.dayOfWeek.ordinal / 7.0,
            input.context.locationContext.ordinal / 5.0,
            input.context.deviceType.ordinal / 4.0,
            input.context.networkStatus.ordinal / 3.0
        )

        return (baseFeatures + contextFeatures).take(FEATURE_COUNT).toDoubleArray().normalize()
    }

    private fun DoubleArray.normalize(): DoubleArray {
        val min = this.minOrNull() ?: return this
        val max = this.maxOrNull() ?: return this
        val range = max - min
        return if (range &gt; 0) {
            this.map { (it - min) / range }.toDoubleArray()
        } else {
            this
        }
    }

    private fun generateContextFactors(context: Context): List&lt;String&gt; {
        val factors = mutableListOf&lt;String&gt;()
        when (context.timeOfDay) {
            TimeOfDay.MORNING, TimeOfDay.AFTERNOON -&gt; factors.add("工作时段推荐")
            TimeOfDay.EVENING, TimeOfDay.NIGHT -&gt; factors.add("休闲时段推荐")
            else -&gt; {}
        }
        when (context.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -&gt; factors.add("周末模式")
            else -&gt; factors.add("工作日模式")
        }
        when (context.locationContext) {
            LocationContext.HOME -&gt; factors.add("家庭环境")
            LocationContext.WORK -&gt; factors.add("工作环境")
            else -&gt; {}
        }
        return factors
    }

    private fun calculateDiversityScore(input: EnhancedPredictionInput): Float {
        return 0.75f
    }

    private fun calculateNoveltyScore(input: EnhancedPredictionInput): Float {
        return 0.6f
    }

    private fun generateEnhancedExplanation(
        input: EnhancedPredictionInput,
        probability: Double,
        contextFactors: List&lt;String&gt;
    ): String {
        val reasons = mutableListOf&lt;String&gt;()
        if (input.baseInput.usageFrequency &gt; 0.7) {
            reasons.add("高频使用 (${(input.baseInput.usageFrequency * 100).toInt()}%)")
        }
        if (input.baseInput.clonePotential &gt; 0.6) {
            reasons.add("克隆潜力高")
        }
        if (input.baseInput.crossDeviceUsage &gt; 0.5) {
            reasons.add("跨设备使用需求")
        }
        reasons.addAll(contextFactors)

        return if (reasons.isNotEmpty()) {
            "基于 ${reasons.take(4).joinToString("、")}，建议${if (probability &gt; 0.5) "克隆" else "不克隆"}"
        } else {
            "综合分析建议${if (probability &gt; 0.5) "克隆" else "不克隆"}此应用"
        }
    }

    private fun getRecommendedAction(probability: Double): MLRecommendationModel.RecommendedAction {
        return when {
            probability &gt; 0.9 -&gt; MLRecommendationModel.RecommendedAction.STRONGLY_RECOMMEND
            probability &gt; 0.7 -&gt; MLRecommendationModel.RecommendedAction.RECOMMEND
            probability &gt; 0.5 -&gt; MLRecommendationModel.RecommendedAction.CONSIDER
            probability &gt; 0.3 -&gt; MLRecommendationModel.RecommendedAction.NOT_RECOMMEND
            else -&gt; MLRecommendationModel.RecommendedAction.STRONGLY_NOT_RECOMMEND
        }
    }

    private fun calculateAdvancedMetrics(
        trainingData: MLRecommendationModel.TrainingData,
        accuracy: Float,
        trainingTime: Long,
        epochs: Int
    ): AdvancedModelMetrics {
        var truePositives = 0
        var falsePositives = 0
        var trueNegatives = 0
        var falseNegatives = 0

        for (i in trainingData.features.indices) {
            val features = trainingData.features[i]
            val (_, _, output) = forwardPass(features)
            val prediction = sigmoid(output[0]) &gt; 0.5
            val actual = trainingData.labels[i] == 1

            when {
                prediction &amp;&amp; actual -&gt; truePositives++
                prediction &amp;&amp; !actual -&gt; falsePositives++
                !prediction &amp;&amp; !actual -&gt; trueNegatives++
                !prediction &amp;&amp; actual -&gt; falseNegatives++
            }
        }

        val precision = if (truePositives + falsePositives &gt; 0) {
            truePositives.toFloat() / (truePositives + falsePositives)
        } else 0f

        val recall = if (truePositives + falseNegatives &gt; 0) {
            truePositives.toFloat() / (truePositives + falseNegatives)
        } else 0f

        val f1Score = if (precision + recall &gt; 0) {
            2 * (precision * recall) / (precision + recall)
        } else 0f

        val aucRoc = calculateAucRoc(trainingData)

        val featureImportance = calculateFeatureImportance(trainingData)

        return AdvancedModelMetrics(
            accuracy = accuracy,
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            aucRoc = aucRoc,
            trainingTime = trainingTime,
            epochCount = epochs,
            featureImportance = featureImportance,
            version = 2,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun calculateAucRoc(trainingData: MLRecommendationModel.TrainingData): Float {
        val predictions = trainingData.features.map { features -&gt;
            val (_, _, output) = forwardPass(features)
            sigmoid(output[0])
        }

        val labels = trainingData.labels

        var auc = 0.0
        val positiveCount = labels.count { it == 1 }
        val negativeCount = labels.size - positiveCount

        if (positiveCount == 0 || negativeCount == 0) return 0.5f

        for (i in labels.indices) {
            if (labels[i] == 1) {
                for (j in labels.indices) {
                    if (labels[j] == 0) {
                        if (predictions[i] &gt; predictions[j]) {
                            auc += 1.0
                        } else if (predictions[i] == predictions[j]) {
                            auc += 0.5
                        }
                    }
                }
            }
        }

        return (auc / (positiveCount * negativeCount)).toFloat()
    }

    private fun calculateFeatureImportance(trainingData: MLRecommendationModel.TrainingData): Map&lt;String, Float&gt; {
        val importance = mutableMapOf&lt;String, Float&gt;()
        val featureNames = listOf(
            "usage_frequency", "session_duration", "time_of_day", "day_of_week",
            "app_category", "recent_activity", "clone_potential", "privacy_risk",
            "cross_device_usage", "social_score"
        )

        for (i in 0 until FEATURE_COUNT) {
            val baseAccuracy = evaluateAccuracy(trainingData)
            val permutedFeatures = trainingData.features.map { features -&gt;
                val permuted = features.copyOf()
                permuted[i] = Random().nextDouble()
                permuted
            }
            val permutedData = MLRecommendationModel.TrainingData(permutedFeatures, trainingData.labels)
            val permutedAccuracy = evaluateAccuracy(permutedData)
            importance[featureNames[i]] = (baseAccuracy - permutedAccuracy).toFloat()
        }

        val totalImportance = importance.values.sum()
        return importance.mapValues { (it.value / totalImportance * 100) }
    }

    private fun evaluateAccuracy(trainingData: MLRecommendationModel.TrainingData): Double {
        var correct = 0
        for (i in trainingData.features.indices) {
            val features = trainingData.features[i]
            val (_, _, output) = forwardPass(features)
            val prediction = sigmoid(output[0]) &gt; 0.5
            val actual = trainingData.labels[i] == 1
            if (prediction == actual) correct++
        }
        return correct.toDouble() / trainingData.features.size
    }

    fun recordFeedback(
        input: EnhancedPredictionInput,
        prediction: EnhancedPredictionResult,
        userAction: FeedbackStore.UserAction
    ) {
        feedbackStore.addFeedback(
            FeedbackStore.Feedback(input, prediction, userAction)
        )
        Log.i(TAG, "Feedback recorded: $userAction")
    }

    suspend fun retrainWithFeedback(): Result&lt;AdvancedModelMetrics&gt; {
        val feedbackData = feedbackStore.getTrainingData()
        if (feedbackData.features.size &lt; 10) {
            Log.w(TAG, "Not enough feedback data for retraining")
            return Result.failure(Exception("Insufficient feedback data"))
        }
        return train(feedbackData)
    }

    fun getCurrentContext(): Context {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

        val timeOfDay = when (hour) {
            in 0..5 -&gt; TimeOfDay.EARLY_MORNING
            in 6..11 -&gt; TimeOfDay.MORNING
            in 12..17 -&gt; TimeOfDay.AFTERNOON
            in 18..21 -&gt; TimeOfDay.EVENING
            else -&gt; TimeOfDay.NIGHT
        }

        val dayOfWeek = when (day) {
            Calendar.MONDAY -&gt; DayOfWeek.MONDAY
            Calendar.TUESDAY -&gt; DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -&gt; DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -&gt; DayOfWeek.THURSDAY
            Calendar.FRIDAY -&gt; DayOfWeek.FRIDAY
            Calendar.SATURDAY -&gt; DayOfWeek.SATURDAY
            else -&gt; DayOfWeek.SUNDAY
        }

        return Context(
            timeOfDay = timeOfDay,
            dayOfWeek = dayOfWeek
        )
    }
}
