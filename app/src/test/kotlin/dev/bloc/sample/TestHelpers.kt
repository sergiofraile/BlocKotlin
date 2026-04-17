package dev.bloc.sample

import dev.bloc.HydratedStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit 4 rule that replaces [Dispatchers.Main] with [UnconfinedTestDispatcher] for the duration
 * of every test. Required because Bloc and Cubit create their coroutine scopes with
 * `Dispatchers.Main.immediate`, which is not available in pure JVM unit tests.
 *
 * Pass in a [TestCoroutineScheduler] to share virtual time across the rule and [runTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val scheduler: TestCoroutineScheduler = TestCoroutineScheduler(),
) : TestWatcher() {
    val testDispatcher = UnconfinedTestDispatcher(scheduler)

    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * A hermetic, dictionary-backed [HydratedStorage] for unit tests.
 * Eliminates any dependency on SharedPreferences or the Android context, keeping tests isolated.
 */
class InMemoryStorage : HydratedStorage {
    private val store = mutableMapOf<String, ByteArray>()

    override fun read(key: String): ByteArray? = store[key]
    override fun write(key: String, value: ByteArray) { store[key] = value }
    override fun delete(key: String) { store.remove(key) }
    override fun clear() { store.clear() }
}
