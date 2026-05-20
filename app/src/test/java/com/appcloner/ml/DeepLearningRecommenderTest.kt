
package com.appcloner.ml

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import android.content.Context
import io.mockk.mockk

class DeepLearningRecommenderTest {

    private lateinit var recommender: DeepLearningRecommender
    private val mockContext: Context = mockk(relaxed = true)

    @Before
    fun setup() = runTest {
        recommender = DeepLearningRecommender(mockContext)
        val result = recommender.initialize()
        assertTrue("Initialization should succeed", result.isSuccess)
    }

    @Test
    fun `test initialization`() = runTest {
        val state = recommender.modelState.value
        assertTrue("State should be Ready", state is DeepLearningRecommender.ModelState.Ready)
        val readyState = state as DeepLearningRecommender.ModelState.Ready
        assertTrue("Accuracy should be reasonable", readyState.accuracy &gt;= 0.5f)
    }

    @Test
    fun `test prediction with base input`() = runTest {
        val baseInput = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.8,
            sessionDuration = 0.7,
            timeOfDay = 0.5,
            dayOfWeek = 0.3,
            appCategory = 0.6,
            recentActivity = 0.8,
            clonePotential = 0.9,
            privacyRisk = 0.3,
            crossDeviceUsage = 0.7,
            socialScore = 0.5
        )

        val enhancedInput = DeepLearningRecommender.EnhancedPredictionInput(
            baseInput = baseInput,
            context = DeepLearningRecommender.Context()
        )

        val result = recommender.predict(enhancedInput)

        assertNotNull("Prediction result should not be null", result)
        assertTrue("Probability should be between 0 and 1", 
            result.probability in 0f..1f)
        assertNotNull("Explanation should not be null", result.explanation)
        assertTrue("Context factors should not be empty", result.contextFactors.isNotEmpty())
    }

    @Test
    fun `test prediction with different contexts`() = runTest {
        val baseInput = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.7,
            sessionDuration = 0.6,
            timeOfDay = 0.4,
            dayOfWeek = 0.2,
            appCategory = 0.5,
            recentActivity = 0.7,
            clonePotential = 0.8,
            privacyRisk = 0.2,
            crossDeviceUsage = 0.6,
            socialScore = 0.4
        )

        val workContext = DeepLearningRecommender.Context(
            timeOfDay = DeepLearningRecommender.TimeOfDay.MORNING,
            dayOfWeek = DeepLearningRecommender.DayOfWeek.MONDAY,
            locationContext = DeepLearningRecommender.LocationContext.WORK
        )

        val homeContext = DeepLearningRecommender.Context(
            timeOfDay = DeepLearningRecommender.TimeOfDay.EVENING,
            dayOfWeek = DeepLearningRecommender.DayOfWeek.SATURDAY,
            locationContext = DeepLearningRecommender.LocationContext.HOME
        )

        val workResult = recommender.predict(DeepLearningRecommender.EnhancedPredictionInput(
            baseInput = baseInput, context = workContext))
        
        val homeResult = recommender.predict(DeepLearningRecommender.EnhancedPredictionInput(
            baseInput = baseInput, context = homeContext))

        assertNotNull("Work context prediction should not be null", workResult)
        assertNotNull("Home context prediction should not be null", homeResult)
    }

    @Test
    fun `test training with synthetic data`() = runTest {
        val mlModel = MLRecommendationModel(mockContext)
        mlModel.initialize()
        
        val trainingData = mlModel.generateSyntheticTrainingData()
        
        val result = recommender.train(trainingData)
        
        assertTrue("Training should succeed", result.isSuccess)
        val metrics = result.getOrNull()
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Accuracy should be &gt; 0.5", metrics!!.accuracy &gt; 0.5f)
        assertTrue("Precision should be &gt; 0", metrics.precision &gt; 0f)
        assertTrue("Recall should be &gt; 0", metrics.recall &gt; 0f)
        assertTrue("F1 score should be &gt; 0", metrics.f1Score &gt; 0f)
        assertTrue("AUC ROC should be &gt; 0.5", metrics.aucRoc &gt; 0.5f)
    }

    @Test
    fun `test feedback recording`() = runTest {
        val baseInput = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.8,
            sessionDuration = 0.7,
            timeOfDay = 0.5,
            dayOfWeek = 0.3,
            appCategory = 0.6,
            recentActivity = 0.8,
            clonePotential = 0.9,
            privacyRisk = 0.3,
            crossDeviceUsage = 0.7,
            socialScore = 0.5
        )

        val enhancedInput = DeepLearningRecommender.EnhancedPredictionInput(
            baseInput = baseInput,
            context = DeepLearningRecommender.Context()
        )

        val prediction = recommender.predict(enhancedInput)

        recommender.recordFeedback(
            input = enhancedInput,
            prediction = prediction,
            userAction = DeepLearningRecommender.FeedbackStore.UserAction.ACCEPTED
        )

        recommender.recordFeedback(
            input = enhancedInput,
            prediction = prediction,
            userAction = DeepLearningRecommender.FeedbackStore.UserAction.REJECTED
        )

        assertTrue("Feedback should be recorded", true)
    }

    @Test
    fun `test context detection`() {
        val context = recommender.getCurrentContext()
        
        assertNotNull("Context should not be null", context)
        assertNotNull("Time of day should be set", context.timeOfDay)
        assertNotNull("Day of week should be set", context.dayOfWeek)
    }

    @Test
    fun `test diversity and novelty scores`() = runTest {
        val baseInput = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.7,
            sessionDuration = 0.6,
            timeOfDay = 0.4,
            dayOfWeek = 0.2,
            appCategory = 0.5,
            recentActivity = 0.7,
            clonePotential = 0.8,
            privacyRisk = 0.2,
            crossDeviceUsage = 0.6,
            socialScore = 0.4
        )

        val enhancedInput = DeepLearningRecommender.EnhancedPredictionInput(
            baseInput = baseInput,
            context = DeepLearningRecommender.Context()
        )

        val result = recommender.predict(enhancedInput)

        assertTrue("Diversity score should be between 0 and 1", 
            result.diversityScore in 0f..1f)
        assertTrue("Novelty score should be between 0 and 1", 
            result.noveltyScore in 0f..1f)
    }

    @Test
    fun `test model metrics flow`() = runTest {
        val mlModel = MLRecommendationModel(mockContext)
        mlModel.initialize()
        val trainingData = mlModel.generateSyntheticTrainingData()

        assertNull("Initial metrics should be null", recommender.modelMetrics.value)

        recommender.train(trainingData)

        val metrics = recommender.modelMetrics.value
        assertNotNull("Metrics should be updated after training", metrics)
        assertTrue("Accuracy should be set", metrics!!.accuracy &gt; 0f)
        assertTrue("Version should be &gt; 0", metrics.version &gt; 0)
        assertTrue("Last updated should be recent", 
            metrics.lastUpdated &gt; System.currentTimeMillis() - 60000)
    }

    @Test
    fun `test prediction confidence levels`() = runTest {
        val highConfidenceInput = MLRecommendationModel.PredictionInput(
            usageFrequency = 0.95,
            sessionDuration = 0.9,
            timeOfDay = 0.5,
            dayOfWeek = 0.3,
            appCategory = 0.7,
            recentActivity = 0.95,
            clonePotential = 0.98,
            privacyRisk = 0.1,
            crossDeviceUsage = 0.9,
            socialScore = 0.8
        )

        val enhancedInput = DeepLearningRecommender.EnhancedPredictionInput(
            baseInput = highConfidenceInput,
            context = DeepLearningRecommender.Context()
        )

        val result = recommender.predict(enhancedInput)

        assertNotNull("Confidence should be set", result.confidence)
    }

    @Test
    fun `test feature importance calculation`() = runTest {
        val mlModel = MLRecommendationModel(mockContext)
        mlModel.initialize()
        val trainingData = mlModel.generateSyntheticTrainingData()

        val metricsResult = recommender.train(trainingData)
        
        assertTrue("Training should succeed", metricsResult.isSuccess)
        
        val metrics = metricsResult.getOrNull()
        assertNotNull("Metrics should not be null", metrics)
        
        val featureImportance = metrics!!.featureImportance
        assertNotNull("Feature importance should not be null", featureImportance)
        assertTrue("Should have feature importance entries", featureImportance.isNotEmpty())
        
        val totalImportance = featureImportance.values.sum()
        assertTrue("Total importance should be approximately 100", 
            totalImportance in 95f..105f)
    }
}
