package dev.bloc.sample.examples.suvs

import android.content.Context
import dev.bloc.sample.examples.suvs.models.InstanceState
import dev.bloc.sample.examples.suvs.models.SuvActiveDirectoryUser
import dev.bloc.sample.examples.suvs.models.SuvErrorResponse
import dev.bloc.sample.examples.suvs.models.SuvifyError
import dev.bloc.sample.examples.suvs.models.SuvInstance
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties

// ─────────────────────────────────────────────────────────────────────────────
// Protocol
// ─────────────────────────────────────────────────────────────────────────────

interface SUVRepositoryProtocol {
    suspend fun login(username: String, password: String): SuvActiveDirectoryUser
    suspend fun fetchInstances(username: String, authToken: String): List<SuvInstance>
    suspend fun extendInstance(instanceId: String, hours: Int, authToken: String): SuvInstance
}

// ─────────────────────────────────────────────────────────────────────────────
// Real repository
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Production repository that delegates to [SUVNetworkService].
 *
 * The Suvify client key is read from `assets/suvify.properties` (gitignored).
 * Copy `assets/suvify.properties.example` to `assets/suvify.properties` and
 * set `suvify_key` to your API key.
 */
class SUVRepository(
    context: Context,
    private val networkService: SUVNetworkService = SUVNetworkService(),
) : SUVRepositoryProtocol {

    private val clientKey: String = loadClientKey(context)

    override suspend fun login(username: String, password: String): SuvActiveDirectoryUser {
        if (username.isBlank()) throw SuvifyError.InvalidCredentials("Username is required")
        if (password.isEmpty()) throw SuvifyError.InvalidCredentials("Password is required")
        return networkService.login(username, password, clientKey)
    }

    override suspend fun fetchInstances(username: String, authToken: String): List<SuvInstance> =
        networkService.fetchInstances(username, authToken)

    override suspend fun extendInstance(
        instanceId: String,
        hours: Int,
        authToken: String,
    ): SuvInstance {
        val newStopTime = Instant.now()
            .plusSeconds(hours * 3600L)
            .atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        return networkService.extendInstance(instanceId, newStopTime, authToken)
    }

    private companion object {
        fun loadClientKey(context: Context): String = runCatching {
            context.assets.open("suvify.properties").use { stream ->
                Properties().also { it.load(stream) }
                    .getProperty("suvify_key", "")
                    .takeIf { it.isNotEmpty() && it != "YOUR_API_KEY_HERE" }
                    ?: ""
            }
        }.getOrDefault("")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mock repository (used in unit tests and when the real key is absent)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Simulates the SUVify backend with plausible data and artificial delays.
 *
 * Used in unit tests and as a fallback when `assets/suvify.properties` is
 * absent or has no key configured.
 */
class MockSUVRepository : SUVRepositoryProtocol {

    private var instances = buildDefaultInstances()

    override suspend fun login(username: String, password: String): SuvActiveDirectoryUser {
        if (username.isBlank() || password.isBlank()) {
            throw SuvifyError.InvalidCredentials("Please enter your credentials")
        }
        return SuvActiveDirectoryUser(
            clientName  = "SUVify",
            userName    = username,
            timeToLive  = Instant.now().plusSeconds(3600).toString(),
            token       = "mock-token-${System.currentTimeMillis()}",
        )
    }

    override suspend fun fetchInstances(username: String, authToken: String): List<SuvInstance> =
        instances

    override suspend fun extendInstance(
        instanceId: String,
        hours: Int,
        authToken: String,
    ): SuvInstance {
        val instance = instances.find { it.instanceId == instanceId }
            ?: throw IllegalArgumentException("Instance not found")
        val newStop = (parseInstant(instance.wdAutoStopTime) ?: Instant.now())
            .plusSeconds(hours * 3600L)
        val extended = instance.copy(wdAutoStopTime = newStop.toString())
        instances = instances.map { if (it.instanceId == instanceId) extended else it }
        return extended
    }

    private fun parseInstant(iso: String?): Instant? =
        iso?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private fun buildDefaultInstances(): List<SuvInstance> {
        val now = Instant.now()
        return listOf(
            mockInstance(
                id          = "inst-001",
                description = "Development Server Alpha — daily build",
                hostname    = "dev-alpha.internal.corp",
                state       = "running",
                autoStop    = now.plusSeconds(3 * 3600),
            ),
            mockInstance(
                id          = "inst-002",
                description = "Development Server Beta — feature branch",
                hostname    = "dev-beta.internal.corp",
                state       = "running",
                autoStop    = now.plusSeconds(45 * 60),
            ),
            mockInstance(
                id          = "inst-003",
                description = "Staging Environment — release candidate",
                hostname    = "staging-01.internal.corp",
                state       = "running",
                autoStop    = now.plusSeconds(7 * 3600),
            ),
            mockInstance(
                id          = "inst-004",
                description = "Automated Test Runner",
                hostname    = "test-runner.internal.corp",
                state       = "stopped",
                autoStop    = now.minusSeconds(2 * 3600),
            ),
        )
    }

    private fun mockInstance(
        id: String,
        description: String,
        hostname: String,
        state: String,
        autoStop: Instant,
    ) = SuvInstance(
        instanceId       = id,
        wdHostname       = hostname,
        wdPassword       = "mock-password",
        wdCurrentState   = null,
        wdAutoStopTime   = autoStop.toString(),
        stateString      = state,
        wdDescription    = description,
    )
}
