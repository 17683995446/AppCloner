package com.appcloner.ml

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class MLRecommendationModelTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var mlModel: MLRecommendationModel

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        prefs = mock(SharedPreferences::class.java)
        prefsEditor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs)
        `when`(prefs.edit()).thenReturn(prefsEditor)
        `when`(prefsEditor.putString(anyString(), any())).thenReturn(prefsEditor)
        `when`(prefsEditor.putStringSet(anyString(), any())).thenReturn(prefsEditor)
        `when`(prefsEditor.putLong(anyString(), anyLong())).thenReturn(prefsEditor)
        `when`(prefsEditor.putFloat(anyString(), anyFloat())).thenReturn(prefsEditor)
        `when`(prefsEditor.putInt(anyString(), anyInt())).thenReturn(prefsEditor)
        `when`(prefsEditor.apply()).then { }

        `when`(prefs.getInt(eq("model_version"), eq(0))).thenReturn(0)
        `when`(prefs.getFloat(eq("model_accuracy"), eq(0.8f))).thenReturn(0.8f)

        mlModel = MLRecommendationModel(context)
    }

    @Test
    fun testInitialState() {
        val state = mlModel.modelState.value
        assertTrue(state is MLRecommendationModel.ModelState.NotInitialized)
    }

    @Test
    fun testInitialization(): Unit = runBlocking {
        val result = mlModel.initialize()

        assertTrue(result.isSuccess)
        assertTrue(mlModel.modelState.value is MLRecommendationModel.ModelState.Ready)
    }

    @Test
    fun testInitializationLoadsExistingModel(): Unit = runBlocking {
        `when`(prefs.getInt(eq("model_version"), eq(0))).thenReturn(1)
        `when`(prefs.getString(eq("model_weights"), isNull())).thenReturn("[0.1,0.2,0.3]")
        `when`(prefs.getFloat(eq("model_bias"), eq(0f))).thenReturn(0.1f)

        val result = mlModel.initialize()

        assertTrue(result.isSuccess)
    }

    @Test
    fun testModelMetricsInitialized(): Unit = runBlocking {
        mlModel.initialize()

        val metrics = mlModel.modelMetrics.value
        assertNotNull(metrics)
        assertTrue(metrics.accuracy >= 0.8f)
    }

    @Test
    fun testTrainingWithSyntheticData(): Unit = runBlocking {
        mlModel.initialize()

        val trainingData = mlModel.generateSyntheticTrainingData()
        val result = mlModel.train(trainingData)

        assertTrue(result.isSuccess)
        val metrics = result.getOrNull()
        assertNotNull(metrics)
        assertTrue(metrics.accuracy > 0)
        assertTrue(metrics.precision >= 0)
        assertTrue(metrics.recall >= 0)
        assertTrue(metrics.f1Score >= 0)
    }

    @Test
    fun testPredictionWithHighUsage(): Unit = runBlocking {
        mlModel.initialize()

        val input = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.9,
            sessionDuration = 0.8,
            timeOfDay = 0.5,
            dayOfWeek = 0.5,
            appCategory = 0.6,
            recentActivity = 0.9,
            clonePotential = 0.8,
            privacyRisk = 0.3,
            crossDeviceUsage = 0.7,
            socialScore = 0.5
        )

        val result = mlModel.predict(input)

        assertNotNull(result)
        assertTrue(result.probability >= 0)
        assertTrue(result.probability <= 1)
        assertNotNull(result.confidence)
        assertNotNull(result.recommendedAction)
        assertTrue(result.explanation.isNotEmpty())
    }

    @Test
    fun testPredictionWithLowUsage(): Unit = runBlocking {
        mlModel.initialize()

        val input = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.1,
            sessionDuration = 0.1,
            timeOfDay = 0.5,
            dayOfWeek = 0.5,
            appCategory = 0.2,
            recentActivity = 0.1,
            clonePotential = 0.2,
            privacyRisk = 0.1,
            crossDeviceUsage = 0.1,
            socialScore = 0.1
        )

        val result = mlModel.predict(input)

        assertNotNull(result)
        assertTrue(result.probability >= 0 && result.probability <= 1)
    }

    @Test
    fun testPredictionShouldClone(): Unit = runBlocking {
        mlModel.initialize()

        val input = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.9,
            sessionDuration = 0.9,
            timeOfDay = 0.8,
            dayOfWeek = 0.7,
            appCategory = 0.8,
            recentActivity = 0.9,
            clonePotential = 0.9,
            privacyRisk = 0.8,
            crossDeviceUsage = 0.9,
            socialScore = 0.8
        )

        val result = mlModel.predict(input)

        assertNotNull(result)
    }

    @Test
    fun testConfidenceLevels() {
        val levels = MLRecommendationModel.ConfidenceLevel.values()
        assertEquals(5, levels.size)
        assertTrue(levels.contains(MLRecommendationModel.ConfidenceLevel.VERY_HIGH))
        assertTrue(levels.contains(MLRecommendationModel.ConfidenceLevel.HIGH))
        assertTrue(levels.contains(MLRecommendationModel.ConfidenceLevel.MEDIUM))
        assertTrue(levels.contains(MLRecommendationModel.ConfidenceLevel.LOW))
        assertTrue(levels.contains(MLRecommendationModel.ConfidenceLevel.VERY_LOW))
    }

    @Test
    fun testRecommendedActions() {
        val actions = MLRecommendationModel.RecommendedAction.values()
        assertEquals(5, actions.size)
        assertTrue(actions.contains(MLRecommendationModel.RecommendedAction.STRONGLY_RECOMMEND))
        assertTrue(actions.contains(MLRecommendationModel.RecommendedAction.RECOMMEND))
        assertTrue(actions.contains(MLRecommendationModel.RecommendedAction.CONSIDER))
        assertTrue(actions.contains(MLRecommendationModel.RecommendedAction.NOT_RECOMMEND))
        assertTrue(actions.contains(MLRecommendationModel.RecommendedAction.STRONGLY_NOT_RECOMMEND))
    }

    @Test
    fun testModelStateTransitions(): Unit = runBlocking {
        assertTrue(mlModel.modelState.value is MLRecommendationModel.ModelState.NotInitialized)

        mlModel.initialize()
        assertTrue(mlModel.modelState.value is MLRecommendationModel.ModelState.Ready)

        val trainingData = mlModel.generateSyntheticTrainingData()
        mlModel.train(trainingData)
        assertTrue(mlModel.modelState.value is MLRecommendationModel.ModelState.Ready)
    }

    @Test
    fun testExportModel(): Unit = runBlocking {
        mlModel.initialize()

        val exported = mlModel.exportModel()

        assertNotNull(exported)
        assertTrue(exported.isNotEmpty())
        assertTrue(exported.contains("version"))
        assertTrue(exported.contains("weights"))
        assertTrue(exported.contains("bias"))
    }

    @Test
    fun testImportModel(): Unit = runBlocking {
        mlModel.initialize()

        val modelJson = """
            {
                "version": 1,
                "weights": [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0],
                "bias": 0.05,
                "accuracy": 0.85
            }
        """.trimIndent()

        val result = mlModel.importModel(modelJson)
        assertTrue(result)
    }

    @Test
    fun testImportInvalidModel(): Unit = runBlocking {
        mlModel.initialize()

        val result = mlModel.importModel("invalid json")
        assertFalse(result)
    }

    @Test
    fun testFeatureNormalization() {
        val input = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.5,
            sessionDuration = 0.5,
            timeOfDay = 0.5,
            dayOfWeek = 0.5,
            appCategory = 0.5,
            recentActivity = 0.5,
            clonePotential = 0.5,
            privacyRisk = 0.5,
            crossDeviceUsage = 0.5,
            socialScore = 0.5
        )

        val prediction = runBlocking { mlModel.predict(input) }
        assertNotNull(prediction)
        assertTrue(prediction.probability >= 0f && prediction.probability <= 1f)
    }

    @Test
    fun testPredictionWithExtremeValues(): Unit = runBlocking {
        mlModel.initialize()

        val maxInput = MLRecommendationModel.PredictionInput(
            usageFrequency = 1.0,
            sessionDuration = 1.0,
            timeOfDay = 1.0,
            dayOfWeek = 1.0,
            appCategory = 1.0,
            recentActivity = 1.0,
            clonePotential = 1.0,
            privacyRisk = 1.0,
            crossDeviceUsage = 1.0,
            socialScore = 1.0
        )

        val maxResult = mlModel.predict(maxInput)
        assertNotNull(maxResult)
        assertTrue(maxResult.probability >= 0 && maxResult.probability <= 1)

        val minInput = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.0,
            sessionDuration = 0.0,
            timeOfDay = 0.0,
            dayOfWeek = 0.0,
            appCategory = 0.0,
            recentActivity = 0.0,
            clonePotential = 0.0,
            privacyRisk = 0.0,
            crossDeviceUsage = 0.0,
            socialScore = 0.0
        )

        val minResult = mlModel.predict(minInput)
        assertNotNull(minResult)
        assertTrue(minResult.probability >= 0 && minResult.probability <= 1)
    }

    @Test
    fun testSyntheticTrainingDataGeneration() {
        val trainingData = mlModel.generateSyntheticTrainingData()

        assertNotNull(trainingData)
        assertTrue(trainingData.features.isNotEmpty())
        assertTrue(trainingData.labels.isNotEmpty())
        assertEquals(trainingData.features.size, trainingData.labels.size)
        assertTrue(trainingData.features.all { it.size == 10 })
        assertTrue(trainingData.labels.all { it == 0 || it == 1 })
    }

    @Test
    fun testTrainingMetricsCalculation(): Unit = runBlocking {
        mlModel.initialize()

        val trainingData = mlModel.generateSyntheticTrainingData()
        val result = mlModel.train(trainingData)

        assertTrue(result.isSuccess)
        val metrics = result.getOrNull()
        assertNotNull(metrics)

        assertTrue(metrics.accuracy in 0f..1f)
        assertTrue(metrics.precision in 0f..1f)
        assertTrue(metrics.recall in 0f..1f)
        assertTrue(metrics.f1Score in 0f..1f)
        assertTrue(metrics.trainingTime > 0)
        assertTrue(metrics.epochCount > 0)
        assertTrue(metrics.featureImportance.isNotEmpty())
    }

    @Test
    fun testFeatureImportanceMapping(): Unit = runBlocking {
        mlModel.initialize()

        val trainingData = mlModel.generateSyntheticTrainingData()
        val result = mlModel.train(trainingData)

        assertTrue(result.isSuccess)
        val metrics = result.getOrNull()
        assertNotNull(metrics)

        val featureImportance = metrics.featureImportance
        assertTrue(featureImportance.containsKey("usage_frequency"))
        assertTrue(featureImportance.containsKey("session_duration"))
        assertTrue(featureImportance.containsKey("time_of_day"))

        val totalImportance = featureImportance.values.sum()
        assertTrue(totalImportance > 0)
    }

    @Test
    fun testMultiplePredictions(): Unit = runBlocking {
        mlModel.initialize()

        repeat(10) {
            val input = MLRecommendationModel.PredictionInput(
                usageFrequency = Math.random(),
                sessionDuration = Math.random(),
                timeOfDay = Math.random(),
                dayOfWeek = Math.random(),
                appCategory = Math.random(),
                recentActivity = Math.random(),
                clonePotential = Math.random(),
                privacyRisk = Math.random(),
                crossDeviceUsage = Math.random(),
                socialScore = Math.random()
            )

            val result = mlModel.predict(input)
            assertNotNull(result)
            assertTrue(result.probability >= 0 && result.probability <= 1)
        }
    }

    @Test
    fun testModelRetraining(): Unit = runBlocking {
        mlModel.initialize()

        val initialMetrics = mlModel.modelMetrics.value

        val trainingData1 = mlModel.generateSyntheticTrainingData()
        mlModel.train(trainingData1)

        val trainingData2 = mlModel.generateSyntheticTrainingData()
        val result2 = mlModel.train(trainingData2)

        assertTrue(result2.isSuccess)
    }

    @Test
    fun testPredictionResultStructure(): Unit = runBlocking {
        mlModel.initialize()

        val input = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.7,
            sessionDuration = 0.6,
            timeOfDay = 0.5,
            dayOfWeek = 0.4,
            appCategory = 0.8,
            recentActivity = 0.9,
            clonePotential = 0.7,
            privacyRisk = 0.4,
            crossDeviceUsage = 0.6,
            socialScore = 0.5
        )

        val result = mlModel.predict(input)

        assertTrue(result.shouldClone == (result.probability > 0.5))
        assertNotNull(result.confidence)
        assertTrue(result.explanation.isNotEmpty())
        assertNotNull(result.recommendedAction)
    }

    @Test
    fun testModelPersistence(): Unit = runBlocking {
        mlModel.initialize()

        val trainingData = mlModel.generateSyntheticTrainingData()
        mlModel.train(trainingData)

        val modelState = mlModel.modelState.value
        assertTrue(modelState is MLRecommendationModel.ModelState.Ready)
    }
}
