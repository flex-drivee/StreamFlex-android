package com.cinetheta.app.ui.settings.testing

import com.cinetheta.domain.provider.Provider
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.models.ProviderResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object TestingUtils {
    open class TestResult(val success: Boolean) {
        companion object {
            val Pass = TestResult(true)
            val Fail = TestResult(false)
        }
    }

    class Logger {
        enum class LogLevel { Normal, Warning, Error }
        data class Message(val level: LogLevel, val message: String) {
            override fun toString(): String {
                val prefix = when(level) {
                    LogLevel.Normal -> ""
                    LogLevel.Warning -> "Warning: "
                    LogLevel.Error -> "Error: "
                }
                return "$prefix$message"
            }
        }

        private val messageLog = mutableListOf<Message>()
        fun getRawLog(): List<Message> = messageLog.toList()
        fun log(message: String) { messageLog.add(Message(LogLevel.Normal, message)) }
        fun warn(message: String) { messageLog.add(Message(LogLevel.Warning, message)) }
        fun error(message: String) { messageLog.add(Message(LogLevel.Error, message)) }
    }

    class TestResultList(val results: List<SearchResult>) : TestResult(true)

    class TestResultProvider(
        val success: Boolean,
        val log: List<Logger.Message>,
        val exception: Throwable?
    )

    private fun fail(message: String): Nothing = throw AssertionError(message)
    private fun assertTrue(message: String, condition: Boolean) { if (!condition) fail(message) }
    private fun assertNotNull(message: String, value: Any?) { if (value == null) fail(message) }

    private suspend fun testSearch(
        provider: Provider,
        testQueries: List<String>,
        logger: Logger
    ): TestResult {
        val searchResults = testQueries.firstNotNullOfOrNull { query ->
            try {
                logger.log("Searching for: $query")
                val results = provider.search(query)
                results.takeIf { it.isNotEmpty() }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                logger.warn("Exception during search '$query': ${e.message}")
                null
            }
        }
        
        return if (searchResults.isNullOrEmpty()) {
            fail("Provider ${provider.name} did not return any search responses")
            TestResult.Fail
        } else {
            logger.log("Search successful, found ${searchResults.size} results.")
            TestResultList(searchResults)
        }
    }

    private suspend fun testLoad(
        provider: Provider,
        result: SearchResult,
        logger: Logger
    ): TestResult {
        try {
            logger.log("Loading details for: ${result.title}")
            val loadResponse = provider.load(result)
            if (loadResponse == null) {
                logger.error("Returned null loadResponse for ${result.title}")
                return TestResult.Fail
            }
            if (!loadResponse.success) {
                logger.error("Load failed: ${loadResponse.error}")
                return TestResult.Fail
            }

            val hasContent = loadResponse.hasSources || loadResponse.hasSeasons
            if (!hasContent) {
                logger.error("Provider got no episodes/sources on ${result.title}")
                return TestResult.Fail
            }

            logger.log("Successfully loaded details and found sources/seasons.")
            return TestResult.Pass
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            logger.error("Exception during load: ${e.message}")
            throw e
        }
    }

    fun getDeferredProviderTests(
        scope: CoroutineScope,
        providers: List<Provider>,
        callback: (Provider, TestResultProvider) -> Unit
    ) {
        providers.forEach { provider ->
            scope.launch {
                val logger = Logger()
                val result = try {
                    logger.log("Trying ${provider.name}...")
                    
                    val searchQueries = listOf("over", "iron", "guy", "man", "movie", "love")
                    
                    val searchResults = testSearch(provider, searchQueries, logger)
                    assertTrue("Failed to get search results", searchResults.success)
                    searchResults as TestResultList

                    val success = searchResults.results.take(3).any { searchResult ->
                        val loadTest = testLoad(provider, searchResult, logger)
                        loadTest.success
                    }

                    if (success) {
                        logger.log("Success ${provider.name}")
                        TestResultProvider(true, logger.getRawLog(), null)
                    } else {
                        logger.error("Load failed for all tested items")
                        TestResultProvider(false, logger.getRawLog(), null)
                    }
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    logger.error("Test failed with exception: ${e.message}")
                    TestResultProvider(false, logger.getRawLog(), e)
                }
                
                callback.invoke(provider, result)
            }
        }
    }
}
