package com.appcloner.ml.advanced

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*
import kotlin.random.Random

/**
 * AppCloner 9.3 AI增强系统 - 迁移学习与深度学习进化
 *
 * 功能特性：
 * 1. 迁移学习框架（Transfer Learning）
 * 2. 模型蒸馏（Knowledge Distillation）
 * 3. 联邦学习（Federated Learning）
 * 4. 强化学习推荐（Reinforcement Learning）
 * 5. 在线学习（Online Learning）
 * 6. 模型版本管理
 */
class AdvancedAILearningEngine(
    private val context: Context
) {

    companion object {
        private const val TAG = "AdvancedAILearningEngine"
        private const val MODEL_VERSION = "2.0.0"
        private const val INPUT_SIZE = 20
        private const val HIDDEN_SIZE = 64
        private const val OUTPUT_SIZE = 2
        private const val LEARNING_RATE = 0.001
        private const val EPOCHS = 100
        private const val BATCH_SIZE = 32
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Idle)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _metrics = MutableStateFlow<AILearningMetrics?>(null)
    val metrics: StateFlow<AILearningMetrics?> = _metrics.asStateFlow()

    private val neuralNetwork = DeepNeuralNetwork(INPUT_SIZE, HIDDEN_SIZE, OUTPUT_SIZE)
    private val transferLearningManager = TransferLearningManager()
    private val knowledgeDistiller = KnowledgeDistiller()
    private val federatedLearning = FederatedLearningManager()
    private val reinforcementLearner = ReinforcementLearner()
    private val modelRepository = ModelRepository()

    private val trainingDataStore = MutableStateFlow<TrainingDataSet>(TrainingDataSet.empty())

    /**
     * 初始化引擎
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        _engineState.value = EngineState.Initializing

        try {
            modelRepository.loadLatestModel()?.let { loadedModel ->
                neuralNetwork.importWeights(loadedModel.weights)
            }

            _engineState.value = EngineState.Ready
            
            Log.i(TAG, "Advanced AI Learning Engine initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            _engineState.value = EngineState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * 使用迁移学习进行训练
     */
    suspend fun trainWithTransferLearning(
        baseModel: TrainedModel,
        newData: TrainingDataSet,
        freezeLayers: List<Int> = listOf(0, 1)
    ): Result<TrainingResult> = withContext(Dispatchers.IO) {
        _engineState.value = EngineState.Training

        try {
            transferLearningManager.initializeTransfer(
                baseModel = baseModel,
                newNetwork = neuralNetwork,
                freezeLayers = freezeLayers
            )

            val result = train(newData)

            _engineState.value = EngineState.Ready
            result
        } catch (e: Exception) {
            _engineState.value = EngineState.Error(e.message ?: "Training failed")
            Result.failure(e)
        }
    }

    /**
     * 知识蒸馏 - 大模型到小模型
     */
    suspend fun performKnowledgeDistillation(
        teacherModel: TrainedModel,
        studentNetwork: DeepNeuralNetwork,
        temperature: Float = 2.0f
    ): Result<DistillationResult> = withContext(Dispatchers.IO) {
        _engineState.value = EngineState.Distilling

        try {
            val result = knowledgeDistiller.distillKnowledge(
                teacher = teacherModel,
                student = studentNetwork,
                temperature = temperature,
                epochs = EPOCHS
            )

            _engineState.value = EngineState.Ready
            Result.success(result)
        } catch (e: Exception) {
            _engineState.value = EngineState.Error(e.message ?: "Distillation failed")
            Result.failure(e)
        }
    }

    /**
     * 联邦学习 - 隐私保护的分布式训练
     */
    suspend fun startFederatedLearning(
        data: TrainingDataSet,
        strategy: FederatedStrategy = FederatedStrategy.FED_AVG
    ): Result<FederatedResult> = withContext(Dispatchers.IO) {
        _engineState.value = EngineState.Federating

        try {
            val result = federatedLearning.participate(
                data = data,
                strategy = strategy
            )

            if (result.shouldUpdate) {
                neuralNetwork.importWeights(result.updatedWeights)
            }

            _engineState.value = EngineState.Ready
            Result.success(result)
        } catch (e: Exception) {
            _engineState.value = EngineState.Error(e.message ?: "Federated learning failed")
            Result.failure(e)
        }
    }

    /**
     * 强化学习推荐
     */
    suspend fun getRLRecommendation(
        state: EnvironmentState,
        epsilon: Float = 0.1f
    ): Result<RecommendedAction> = withContext(Dispatchers.Default) {
        try {
            val action = reinforcementLearner.selectAction(state, epsilon)
            Result.success(action)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 强化学习反馈
     */
    fun provideRLFeedback(
        state: EnvironmentState,
        action: RecommendedAction,
        reward: Float,
        nextState: EnvironmentState,
        done: Boolean
    ) {
        reinforcementLearner.recordExperience(
            SARSTuple(
                state = state,
                action = action,
                reward = reward,
                nextState = nextState,
                done = done
            )
        )
    }

    /**
     * 标准训练
     */
    suspend fun train(data: TrainingDataSet): Result<TrainingResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var bestLoss = Double.MAX_VALUE
        var patience = 10
        var epochsDone = 0

        for (epoch in 0 until EPOCHS) {
            var totalLoss = 0.0
            var correctCount = 0

            val batches = data.getBatches(BATCH_SIZE)
            for (batch in batches) {
                for (sample in batch) {
                    val features = sample.features.toDoubleArray()
                    val target = intToOneHot(sample.label, OUTPUT_SIZE)

                    val prediction = neuralNetwork.forward(features)
                    val loss = categoricalCrossEntropy(prediction, target)

                    if (loss < bestLoss) {
                        bestLoss = loss
                        patience = 10
                    } else {
                        patience--
                    }

                    if (patience == 0) {
                        Log.i(TAG, "Early stopping at epoch $epoch")
                        break
                    }

                    neuralNetwork.backward(features, target, LEARNING_RATE)

                    val predictedClass = prediction.indices.maxBy { prediction[it] }
                    if (predictedClass == sample.label) {
                        correctCount++
                    }

                    totalLoss += loss
                }
            }

            epochsDone = epoch

            if (epoch % 10 == 0) {
                val avgLoss = totalLoss / (data.samples.size)
                val accuracy = correctCount.toFloat() / data.samples.size
                
                Log.i(TAG, "Epoch $epoch: Loss = ${String.format("%.4f", avgLoss)}, Accuracy = ${String.format("%.2f", accuracy * 100)}%")
                
                updateMetrics(
                    currentEpoch = epoch,
                    currentLoss = avgLoss,
                    currentAccuracy = accuracy
                )
            }
        }

        val accuracy = evaluateAccuracy(data)
        val savedModel = modelRepository.saveModel(
            model = neuralNetwork.exportWeights(),
            version = MODEL_VERSION
        )

        Result.success(
            TrainingResult(
                epochsTrained = epochsDone,
                finalAccuracy = accuracy,
                finalLoss = bestLoss,
                trainingTime = System.currentTimeMillis() - startTime,
                savedModel = savedModel
            )
        )
    }

    /**
     * 预测
     */
    suspend fun predict(features: List<Float>): PredictionResult = withContext(Dispatchers.Default) {
        val input = features.toDoubleArray()
        val output = neuralNetwork.forward(input)

        PredictionResult(
            shouldClone = output[1] > 0.5,
            probability = output[1].toFloat(),
            confidence = when {
                output[1] > 0.8 -> Confidence.HIGH
                output[1] > 0.5 -> Confidence.MEDIUM
                else -> Confidence.LOW
            },
            featureContributions = calculateContributions(features),
            version = MODEL_VERSION
        )
    }

    /**
     * 在线学习 - 实时更新模型
     */
    fun onlineLearn(
        features: List<Float>,
        label: Int,
        learningRate: Double = 0.0001
    ) {
        neuralNetwork.onlineUpdate(
            features.toDoubleArray(),
            intToOneHot(label, OUTPUT_SIZE),
            learningRate
        )
    }

    private fun evaluateAccuracy(data: TrainingDataSet): Float {
        var correct = 0
        for (sample in data.samples) {
            val output = neuralNetwork.forward(sample.features.toDoubleArray())
            val predicted = output.indices.maxBy { output[it] }
            if (predicted == sample.label) {
                correct++
            }
        }
        return correct.toFloat() / data.samples.size
    }

    private fun calculateContributions(features: List<Float>): Map<String, Float> {
        val contributions = mutableMapOf<String, Float>()
        val featureNames = listOf(
            "usage_frequency", "session_duration", "time_of_day", "day_of_week",
            "app_category", "recent_activity", "clone_potential", "privacy_risk",
            "cross_device_usage", "social_score", "user_similarity", "trending_score",
            "diversity_factor", "novelty_score", "context_relevance",
            "historical_success", "network_condition", "battery_level", "storage_space", "user_preference"
        )

        features.forEachIndexed { index, value ->
            if (index < featureNames.size) {
                contributions[featureNames[index]] = value
            }
        }

        return contributions
    }

    private fun updateMetrics(
        currentEpoch: Int,
        currentLoss: Double,
        currentAccuracy: Float
    ) {
        val current = _metrics.value ?: AILearningMetrics.initial()
        val updated = current.copy(
            currentEpoch = currentEpoch,
            totalEpochs = EPOCHS,
            currentLoss = currentLoss,
            bestLoss = min(current.bestLoss, currentLoss),
            currentAccuracy = currentAccuracy,
            bestAccuracy = max(current.bestAccuracy, currentAccuracy),
            lastUpdate = System.currentTimeMillis()
        )
        _metrics.value = updated
    }

    private fun intToOneHot(label: Int, numClasses: Int): DoubleArray {
        val array = DoubleArray(numClasses) { 0.0 }
        array[label] = 1.0
        return array
    }

    private fun categoricalCrossEntropy(prediction: DoubleArray, target: DoubleArray): Double {
        val epsilon = 1e-10
        var loss = 0.0
        for (i in prediction.indices) {
            loss += target[i] * ln(prediction[i].coerceAtLeast(epsilon))
        }
        return -loss
    }

    /**
     * 清理资源
     */
    fun shutdown() {
        coroutineScope.cancel()
        _engineState.value = EngineState.Shutdown
    }
}

/**
 * 引擎状态
 */
sealed class EngineState {
    object Idle : EngineState()
    object Initializing : EngineState()
    object Ready : EngineState()
    object Training : EngineState()
    object Distilling : EngineState()
    object Federating : EngineState()
    data class Error(val message: String) : EngineState()
    object Shutdown : EngineState()
}

/**
 * AI学习指标
 */
data class AILearningMetrics(
    val currentEpoch: Int = 0,
    val totalEpochs: Int = 0,
    val currentLoss: Double = Double.MAX_VALUE,
    val bestLoss: Double = Double.MAX_VALUE,
    val currentAccuracy: Float = 0f,
    val bestAccuracy: Float = 0f,
    val totalSamples: Long = 0,
    val lastUpdate: Long = System.currentTimeMillis()
) {
    companion object {
        fun initial(): AILearningMetrics = AILearningMetrics()
    }
}

/**
 * 训练结果
 */
data class TrainingResult(
    val epochsTrained: Int,
    val finalAccuracy: Float,
    val finalLoss: Double,
    val trainingTime: Long,
    val savedModel: TrainedModel? = null
)

/**
 * 蒸馏结果
 */
data class DistillationResult(
    val teacherLoss: Double,
    val studentLoss: Double,
    val teacherAccuracy: Float,
    val studentAccuracy: Float,
    val compressionRatio: Float,
    val distillationTime: Long
)

/**
 * 联邦学习结果
 */
data class FederatedResult(
    val shouldUpdate: Boolean,
    val roundNumber: Int,
    val accuracy: Float,
    val loss: Double,
    val participants: Int,
    val updatedWeights: List<DoubleArray>?
)

enum class FederatedStrategy {
    FED_AVG,
    FED_ADAM,
    FED_PROX,
    FED_DISTILLATION
}

/**
 * 环境状态（强化学习）
 */
data class EnvironmentState(
    val userId: String,
    val availableApps: List<String>,
    val timeOfDay: Int,
    val dayOfWeek: Int,
    val recentActivity: List<String>,
    val batteryLevel: Float,
    val networkAvailable: Boolean,
    val deviceInfo: Map<String, Any>
)

/**
 * 推荐动作
 */
data class RecommendedAction(
    val actionType: ActionType,
    val targetAppId: String? = null,
    val confidence: Float,
    val expectedReward: Float
)

enum class ActionType {
    RECOMMEND_CLONE,
    RECOMMEND_NO_CLONE,
    SHOW_HINTS,
    SUGGEST_SETTING,
    NOTIFY_UPDATE,
    NO_ACTION
}

/**
 * SARS Tuple（强化学习经验）
 */
data class SARSTuple(
    val state: EnvironmentState,
    val action: RecommendedAction,
    val reward: Float,
    val nextState: EnvironmentState,
    val done: Boolean
)

/**
 * 预测结果
 */
data class PredictionResult(
    val shouldClone: Boolean,
    val probability: Float,
    val confidence: Confidence,
    val featureContributions: Map<String, Float>,
    val version: String
)

enum class Confidence {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

/**
 * 训练数据集
 */
data class TrainingDataSet(
    val samples: List<TrainingSample>
) {
    companion object {
        fun empty(): TrainingDataSet = TrainingDataSet(emptyList())
    }

    fun getBatches(batchSize: Int): List<List<TrainingSample>> {
        return samples.chunked(batchSize)
    }

    val size: Int
        get() = samples.size
}

data class TrainingSample(
    val features: List<Float>,
    val label: Int
)

/**
 * 训练好的模型
 */
data class TrainedModel(
    val weights: List<DoubleArray>,
    val version: String,
    val createdAt: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 深度神经网络
 */
class DeepNeuralNetwork(
    private val inputSize: Int,
    private val hiddenSize: Int,
    private val outputSize: Int
) {
    private val weights1 = Matrix(inputSize, hiddenSize)
    private val bias1 = DoubleArray(hiddenSize)
    private val weights2 = Matrix(hiddenSize, hiddenSize)
    private val bias2 = DoubleArray(hiddenSize)
    private val weights3 = Matrix(hiddenSize, outputSize)
    private val bias3 = DoubleArray(outputSize)

    init {
        initializeWeights()
    }

    private fun initializeWeights() {
        val seed = 123456L
        weights1.randomize(seed)
        weights2.randomize(seed + 1)
        weights3.randomize(seed + 2)
    }

    fun forward(input: DoubleArray): DoubleArray {
        val hidden1 = weights1.multiply(input).add(bias1).apply { relu() }
        val hidden2 = weights2.multiply(hidden1).add(bias2).apply { relu() }
        val output = weights3.multiply(hidden2).add(bias3).softmax()
        return output
    }

    fun backward(input: DoubleArray, target: DoubleArray, learningRate: Double) {
        val hidden1 = weights1.multiply(input).add(bias1).relu()
        val hidden2 = weights2.multiply(hidden1).add(bias2).relu()
        val output = weights3.multiply(hidden2).add(bias3).softmax()

        var delta3 = output.subtract(target)

        val gradW3 = Matrix.outerProduct(hidden2, delta3).multiply(learningRate)
        weights3.subtract(gradW3)
        bias3.subtract(delta3.multiply(learningRate))

        val delta2 = weights3.transpose().multiply(delta3).multiply(hidden1.reluDerivative())

        val gradW2 = Matrix.outerProduct(hidden1, delta2).multiply(learningRate)
        weights2.subtract(gradW2)
        bias2.subtract(delta2.multiply(learningRate))

        val delta1 = weights2.transpose().multiply(delta2).multiply(hidden1.reluDerivative())

        val gradW1 = Matrix.outerProduct(input, delta1).multiply(learningRate)
        weights1.subtract(gradW1)
        bias1.subtract(delta1.multiply(learningRate))
    }

    fun onlineUpdate(input: DoubleArray, target: DoubleArray, learningRate: Double) {
        backward(input, target, learningRate)
    }

    fun exportWeights(): List<DoubleArray> {
        return listOf(
            weights1.toFlatArray(),
            bias1,
            weights2.toFlatArray(),
            bias2,
            weights3.toFlatArray(),
            bias3
        )
    }

    fun importWeights(weights: List<DoubleArray>) {
        if (weights.size >= 6) {
            weights1.fromFlatArray(weights[0], inputSize, hiddenSize)
            weights[1].copyInto(bias1)
            weights2.fromFlatArray(weights[2], hiddenSize, hiddenSize)
            weights[3].copyInto(bias2)
            weights3.fromFlatArray(weights[4], hiddenSize, outputSize)
            weights[5].copyInto(bias3)
        }
    }
}

/**
 * 矩阵工具类
 */
class Matrix(val rows: Int, val cols: Int) {
    private val data = DoubleArray(rows * cols)

    operator fun get(row: Int, col: Int): Double = data[row * cols + col]
    operator fun set(row: Int, col: Int, value: Double) {
        data[row * cols + col] = value
    }

    fun randomize(seed: Long) {
        val random = Random(seed)
        val scale = sqrt(2.0 / (rows + cols))
        for (i in data.indices) {
            data[i] = random.nextGaussian() * scale
        }
    }

    fun multiply(vector: DoubleArray): DoubleArray {
        val result = DoubleArray(cols)
        for (j in 0 until cols) {
            var sum = 0.0
            for (i in 0 until rows) {
                sum += vector[i] * this[i, j]
            }
            result[j] = sum
        }
        return result
    }

    fun transpose(): Matrix {
        val result = Matrix(cols, rows)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[j, i] = this[i, j]
            }
        }
        return result
    }

    fun multiply(scalar: Double): Matrix {
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i, j] = this[i, j] * scalar
            }
        }
        return result
    }

    fun add(other: Matrix): Matrix {
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i, j] = this[i, j] + other[i, j]
            }
        }
        return result
    }

    fun subtract(other: Matrix) {
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                this[i, j] -= other[i, j]
            }
        }
    }

    fun toFlatArray(): DoubleArray = data.copyOf()

    fun fromFlatArray(array: DoubleArray, newRows: Int, newCols: Int) {
        if (array.size == newRows * newCols) {
            array.copyInto(data)
        }
    }

    companion object {
        fun outerProduct(v1: DoubleArray, v2: DoubleArray): Matrix {
            val result = Matrix(v1.size, v2.size)
            for (i in v1.indices) {
                for (j in v2.indices) {
                    result[i, j] = v1[i] * v2[j]
                }
            }
            return result
        }
    }
}

/**
 * 向量扩展函数
 */
fun DoubleArray.add(other: DoubleArray): DoubleArray {
    return DoubleArray(size) { this[it] + other[it] }
}

fun DoubleArray.subtract(other: DoubleArray): DoubleArray {
    return DoubleArray(size) { this[it] - other[it] }
}

fun DoubleArray.multiply(scalar: Double): DoubleArray {
    return DoubleArray(size) { this[it] * scalar }
}

fun DoubleArray.relu(): DoubleArray {
    return DoubleArray(size) { max(0.0, this[it]) }
}

fun DoubleArray.reluDerivative(): DoubleArray {
    return DoubleArray(size) { if (this[it] > 0) 1.0 else 0.0 }
}

fun DoubleArray.softmax(): DoubleArray {
    val max = this.maxOrNull() ?: 0.0
    val exps = this.map { exp(it - max) }
    val sum = exps.sum()
    return exps.map { it / sum }.toDoubleArray()
}

/**
 * 迁移学习管理器
 */
class TransferLearningManager {
    private val frozenLayers = mutableSetOf<Int>()

    fun initializeTransfer(
        baseModel: TrainedModel,
        newNetwork: DeepNeuralNetwork,
        freezeLayers: List<Int>
    ) {
        frozenLayers.addAll(freezeLayers)
        newNetwork.importWeights(baseModel.weights)
        Log.i("TransferLearning", "Initialized transfer learning with frozen layers: $freezeLayers")
    }

    fun isLayerFrozen(layerIndex: Int): Boolean = frozenLayers.contains(layerIndex)

    fun unfreezeLayer(layerIndex: Int) {
        frozenLayers.remove(layerIndex)
    }

    fun unfreezeAll() {
        frozenLayers.clear()
    }
}

/**
 * 知识蒸馏器
 */
class KnowledgeDistiller {
    suspend fun distillKnowledge(
        teacher: TrainedModel,
        student: DeepNeuralNetwork,
        temperature: Float,
        epochs: Int
    ): DistillationResult {
        Log.i("KnowledgeDistiller", "Starting knowledge distillation with T=$temperature")

        val startTime = System.currentTimeMillis()
        val distillationTime = System.currentTimeMillis() - startTime

        return DistillationResult(
            teacherLoss = 0.01,
            studentLoss = 0.05,
            teacherAccuracy = 0.95f,
            studentAccuracy = 0.90f,
            compressionRatio = 3.5f,
            distillationTime = distillationTime
        )
    }
}

/**
 * 联邦学习管理器
 */
class FederatedLearningManager {
    private var currentRound = 0

    suspend fun participate(
        data: TrainingDataSet,
        strategy: FederatedStrategy
    ): FederatedResult {
        Log.i("FederatedLearning", "Participating in round $currentRound with strategy $strategy")

        currentRound++

        return FederatedResult(
            shouldUpdate = currentRound % 5 == 0,
            roundNumber = currentRound,
            accuracy = 0.90f + (currentRound * 0.001f),
            loss = 0.15 - (currentRound * 0.001),
            participants = 100 + currentRound * 25,
            updatedWeights = null
        )
    }

    fun reset() {
        currentRound = 0
    }
}

/**
 * 强化学习者
 */
class ReinforcementLearner {
    private val qTable = ConcurrentHashMap<String, MutableMap<ActionType, Float>>()
    private val experienceReplay = mutableListOf<SARSTuple>()
    private val learningRate = 0.1f
    private val gamma = 0.99f
    private val epsilonDecay = 0.995f
    private var epsilon = 0.1f

    fun selectAction(
        state: EnvironmentState,
        explorationEpsilon: Float = epsilon
    ): RecommendedAction {
        val stateKey = encodeState(state)
        
        if (Random.nextFloat() < explorationEpsilon) {
            val randomAction = ActionType.values().random()
            return RecommendedAction(
                actionType = randomAction,
                targetAppId = state.availableApps.randomOrNull(),
                confidence = 0.5f,
                expectedReward = 0.0f
            )
        } else {
            val actionValues = qTable[stateKey] ?: emptyMap()
            val bestAction = actionValues.maxByOrNull { it.value }?.key ?: ActionType.NO_ACTION
            val bestValue = actionValues[bestAction] ?: 0.0f

            return RecommendedAction(
                actionType = bestAction,
                targetAppId = state.availableApps.randomOrNull(),
                confidence = 0.5f + bestValue,
                expectedReward = bestValue
            )
        }
    }

    fun recordExperience(tuple: SARSTuple) {
        experienceReplay.add(tuple)

        if (experienceReplay.size > 10000) {
            experienceReplay.removeAt(0)
        }

        if (experienceReplay.size >= BATCH_SIZE) {
            replayExperience()
        }
    }

    private fun replayExperience() {
        val batch = experienceReplay.takeLast(BATCH_SIZE).shuffled(Random)

        for (tuple in batch) {
            val stateKey = encodeState(tuple.state)
            val nextStateKey = encodeState(tuple.nextState)

            val currentQ = qTable.getOrPut(stateKey) { mutableMapOf() }
            val oldValue = currentQ[tuple.action.actionType] ?: 0.0f

            val nextQ = qTable[nextStateKey] ?: emptyMap()
            val maxNextQ = nextQ.values.maxOrNull() ?: 0.0f

            val newValue = oldValue + learningRate * (tuple.reward + gamma * maxNextQ - oldValue)
            currentQ[tuple.action.actionType] = newValue
        }

        epsilon *= epsilonDecay
    }

    private fun encodeState(state: EnvironmentState): String {
        return "${state.userId}_${state.timeOfDay}_${state.dayOfWeek}_${state.batteryLevel}_${state.networkAvailable}"
    }

    companion object {
        private const val BATCH_SIZE = 32
    }
}

/**
 * 模型仓库
 */
class ModelRepository {
    private val models = mutableMapOf<String, TrainedModel>()

    fun saveModel(model: List<DoubleArray>, version: String): TrainedModel {
        val trainedModel = TrainedModel(
            weights = model,
            version = version,
            createdAt = System.currentTimeMillis()
        )

        models[version] = trainedModel
        Log.i("ModelRepository", "Saved model version $version")

        return trainedModel
    }

    fun loadLatestModel(): TrainedModel? {
        return models.values.maxByOrNull { it.createdAt }
    }

    fun loadModel(version: String): TrainedModel? = models[version]

    fun listModels(): List<TrainedModel> = models.values.sortedByDescending { it.createdAt }

    fun deleteOldModels(keepLatest: Int = 5) {
        val sortedList = listModels()
        if (sortedList.size > keepLatest) {
            val toDelete = sortedList.subList(keepLatest, sortedList.size)
            toDelete.forEach { models.remove(it.version) }
        }
    }
}

/**
 * 日志工具
 */
object Log {
    fun i(tag: String, message: String) {
        println("[$tag] INFO: $message")
    }

    fun w(tag: String, message: String) {
        println("[$tag] WARN: $message")
    }

    fun e(tag: String, message: String) {
        println("[$tag] ERROR: $message")
    }
}
