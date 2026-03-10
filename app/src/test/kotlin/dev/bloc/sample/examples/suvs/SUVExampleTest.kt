package dev.bloc.sample.examples.suvs

import dev.bloc.sample.MainDispatcherRule
import dev.bloc.sample.examples.suvs.models.SuvActiveDirectoryUser
import dev.bloc.sample.examples.suvs.models.SuvInstance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Tests for [SUVBloc] — the auth flow / repository pattern / DI showcase.
 *
 * The real MockSUVRepository has artificial delays. Tests here use zero-delay
 * mock repositories so async state transitions complete after [advanceUntilIdle]
 * with no real waiting.
 *
 * Mirrors the iOS SUVExampleTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SUVExampleTest {

    @get:Rule val dispatcher = MainDispatcherRule()

    // ─────────────────────────────────────────────────────────────────────────
    // Mock data
    // ─────────────────────────────────────────────────────────────────────────

    private val mockUser = SuvActiveDirectoryUser(
        clientName  = "SUVify",
        userName    = "test.user",
        timeToLive  = Instant.now().plusSeconds(3600).toString(),
        token       = "tok-abc",
    )

    private val mockInstances = listOf(
        SuvInstance(
            instanceId    = "suv-001",
            wdHostname    = "alpha.corp",
            wdPassword    = "pw",
            stateString   = "running",
            wdDescription = "Dev Alpha",
            wdAutoStopTime = Instant.now().plusSeconds(3600).toString(),
        ),
        SuvInstance(
            instanceId    = "suv-002",
            wdHostname    = "beta.corp",
            wdPassword    = "pw",
            stateString   = "stopped",
            wdDescription = "Dev Beta",
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Mock repositories
    // ─────────────────────────────────────────────────────────────────────────

    private inner class SuccessRepository : SUVRepositoryProtocol {
        override suspend fun login(username: String, password: String) = mockUser
        override suspend fun fetchInstances(username: String, authToken: String) = mockInstances
        override suspend fun extendInstance(instanceId: String, hours: Int, authToken: String) =
            mockInstances.first { it.instanceId == instanceId }
    }

    private inner class FailingAuthRepository : SUVRepositoryProtocol {
        override suspend fun login(username: String, password: String): SuvActiveDirectoryUser =
            throw IllegalArgumentException("Invalid credentials")
        override suspend fun fetchInstances(username: String, authToken: String) = emptyList<SuvInstance>()
        override suspend fun extendInstance(instanceId: String, hours: Int, authToken: String): SuvInstance =
            throw IllegalStateException("Not reached")
    }

    private inner class FailingInstancesRepository : SUVRepositoryProtocol {
        override suspend fun login(username: String, password: String) = mockUser
        override suspend fun fetchInstances(username: String, authToken: String): List<SuvInstance> =
            throw Exception("Instances unavailable")
        override suspend fun extendInstance(instanceId: String, hours: Int, authToken: String): SuvInstance =
            throw IllegalStateException("Not reached")
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test fun `SUVBloc starts in Initial state`() {
        val bloc = SUVBloc(SuccessRepository())
        assertTrue(bloc.state is SUVState.Initial)
        bloc.close()
    }

    // ── Login transitions ─────────────────────────────────────────────────────

    @Test fun `Successful login ends in Loaded state after async steps complete`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(SuccessRepository())
        bloc.send(SUVEvent.Login("test.user", "pw"))
        advanceUntilIdle()

        val loaded = bloc.state as? SUVState.Loaded
        assertEquals(mockUser.userName, loaded?.user?.userName)
        assertEquals(mockInstances, loaded?.instances)
        bloc.close()
    }

    // ── Login failure ─────────────────────────────────────────────────────────

    @Test fun `Login failure emits AuthError state and publishes on errorsFlow`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(FailingAuthRepository())
        val errors = mutableListOf<Throwable>()
        val job = launch { bloc.errorsFlow.toList(errors) }

        bloc.send(SUVEvent.Login("bad.user", "wrong"))
        advanceUntilIdle()

        assertTrue(bloc.state is SUVState.AuthError)
        assertEquals(1, errors.size)
        job.cancel()
        bloc.close()
    }

    @Test fun `Instance fetch failure after auth emits Error state`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(FailingInstancesRepository())
        bloc.send(SUVEvent.Login("test.user", "pw"))
        advanceUntilIdle()

        assertTrue(bloc.state is SUVState.Error)
        bloc.close()
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test fun `Logout resets state back to Initial`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(SuccessRepository())
        bloc.send(SUVEvent.Login("test.user", "pw"))
        advanceUntilIdle()

        bloc.send(SUVEvent.Logout)
        assertTrue(bloc.state is SUVState.Initial)
        bloc.close()
    }

    // ── RefreshInstances ──────────────────────────────────────────────────────

    @Test fun `RefreshInstances re-fetches instances and updates Loaded state`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(SuccessRepository())
        bloc.send(SUVEvent.Login("test.user", "pw"))
        advanceUntilIdle()

        bloc.send(SUVEvent.RefreshInstances)
        advanceUntilIdle()

        val loaded = bloc.state as? SUVState.Loaded
        assertEquals(mockInstances.size, loaded?.instances?.size)
        bloc.close()
    }

    @Test fun `RefreshInstances is a no-op when currentUser is null`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(SuccessRepository())
        bloc.send(SUVEvent.RefreshInstances)
        advanceUntilIdle()
        assertTrue(bloc.state is SUVState.Initial)
        bloc.close()
    }

    // ── currentUser helper ────────────────────────────────────────────────────

    @Test fun `currentUser is null in Initial state`() {
        val bloc = SUVBloc(FailingAuthRepository())
        assertNull(bloc.state.currentUser)
        bloc.close()
    }

    @Test fun `currentUser is non-null after successful authentication`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(SuccessRepository())
        bloc.send(SUVEvent.Login("test.user", "pw"))
        advanceUntilIdle()

        assertEquals(mockUser.userName, bloc.state.currentUser?.userName)
        bloc.close()
    }

    @Test fun `isAuthenticated is false before login`() {
        val bloc = SUVBloc(SuccessRepository())
        assertTrue(!bloc.state.isAuthenticated)
        bloc.close()
    }

    @Test fun `isAuthenticated is true after successful login`() = runTest(dispatcher.testDispatcher) {
        val bloc = SUVBloc(SuccessRepository())
        bloc.send(SUVEvent.Login("test.user", "pw"))
        advanceUntilIdle()

        assertTrue(bloc.state.isAuthenticated)
        bloc.close()
    }
}
