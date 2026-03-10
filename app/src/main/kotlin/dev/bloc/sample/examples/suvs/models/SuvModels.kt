package dev.bloc.sample.examples.suvs.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

// ─────────────────────────────────────────────────────────────────────────────
// Auth
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class SuvActiveDirectoryUser(
    val clientName: String,
    val userName: String,
    val timeToLive: String,
    val token: String,
) {
    val displayName: String get() = userName
}

// ─────────────────────────────────────────────────────────────────────────────
// Instance state enum
// ─────────────────────────────────────────────────────────────────────────────

enum class InstanceState(val displayName: String, val isActive: Boolean) {
    RUNNING("Running", true),
    BUILDING("Building", true),
    PENDING("Pending", true),
    STOPPING("Stopping", false),
    STOPPED("Stopped", false),
    TERMINATED("Terminated", false),
    SHUTTING_DOWN("Shutting Down", false),
    IMPAIRED("Impaired", false),
    UNKNOWN("Unknown", false);

    companion object {
        fun fromString(value: String): InstanceState = when (value.lowercase()) {
            "running"      -> RUNNING
            "building"     -> BUILDING
            "pending"      -> PENDING
            "stopping"     -> STOPPING
            "stopped"      -> STOPPED
            "terminated"   -> TERMINATED
            "shutting-down"-> SHUTTING_DOWN
            "impaired"     -> IMPAIRED
            else           -> UNKNOWN
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Instance
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class SuvInstance(
    val instanceId: String,
    val wdHostname: String,
    val wdPassword: String,
    val wdCurrentState: String? = null,
    val wdAutoStopTime: String? = null,
    @SerialName("state") val stateString: String,
    val wdDescription: String,
    val wdAutoRestartTime: String? = null,
    val wdAutoTerminateTime: String? = null,
) {
    val id: String get() = instanceId
    val description: String get() = wdDescription
    val hostname: String get() = wdHostname

    val state: InstanceState get() = InstanceState.fromString(stateString)

    val formattedAutoStop: String?
        get() = wdAutoStopTime?.let { stopTimeStr ->
            runCatching {
                val remaining = Instant.parse(stopTimeStr).toEpochMilli() - System.currentTimeMillis()
                when {
                    remaining <= 0          -> "Expired"
                    remaining < 3_600_000   -> "${remaining / 60_000}m remaining"
                    else                    -> "${remaining / 3_600_000}h ${(remaining % 3_600_000) / 60_000}m remaining"
                }
            }.getOrNull()
        }

    val autoStopColor: AutoStopColor
        get() {
            val remaining = wdAutoStopTime?.let { stopTimeStr ->
                runCatching {
                    Instant.parse(stopTimeStr).toEpochMilli() - System.currentTimeMillis()
                }.getOrNull()
            } ?: return AutoStopColor.OK
            return when {
                remaining <= 0          -> AutoStopColor.EXPIRED
                remaining < 3_600_000   -> AutoStopColor.CRITICAL
                remaining < 7_200_000   -> AutoStopColor.WARNING
                else                    -> AutoStopColor.OK
            }
        }
}

enum class AutoStopColor { OK, WARNING, CRITICAL, EXPIRED }

// ─────────────────────────────────────────────────────────────────────────────
// Error response
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class SuvErrorResponse(
    val message: String,
    val detail: String,
    val traceId: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Domain errors
// ─────────────────────────────────────────────────────────────────────────────

sealed class SuvifyError(message: String) : Exception(message) {
    data object InvalidUrlComponents : SuvifyError("Failed to build URL")
    data class DecodingError(val details: String) : SuvifyError("Failed to parse response: $details")
    data class ErrorResponse(val response: SuvErrorResponse) : SuvifyError(response.message)
    data class Unauthorized(val response: SuvErrorResponse) : SuvifyError("Unauthorized: ${response.message}")
    data object UserNotFound : SuvifyError("User not found")
    data object SomethingWentWrong : SuvifyError("Something went wrong. Please try again.")
    data class NetworkError(val detail: String) : SuvifyError("Network error: $detail")
    data class InvalidCredentials(val detail: String) : SuvifyError(detail)
}
