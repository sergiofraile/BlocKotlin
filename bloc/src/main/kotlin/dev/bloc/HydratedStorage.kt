package dev.bloc

/**
 * Persistence contract for [HydratedBloc].
 *
 * The interface is intentionally minimal and KMP-ready — no Android types are imported.
 * Implementations provide the platform-specific storage backend:
 *
 * - Android: [SharedPreferencesStorage] (bundled in the `:bloc` module)
 * - Tests: [InMemoryStorage]
 * - KMP: supply a `settings`-based or SQLite implementation in each target's `actual`
 *
 * ```kotlin
 * // Use the built-in Android implementation
 * HydratedBloc.storage = SharedPreferencesStorage(context)
 *
 * // Swap in a test double
 * HydratedBloc.storage = InMemoryStorage()
 * ```
 */
interface HydratedStorage {
    /** Returns the raw bytes stored under [key], or `null` if absent. */
    fun read(key: String): ByteArray?

    /** Persists [value] under [key], overwriting any previous entry. */
    fun write(key: String, value: ByteArray)

    /** Removes the entry for [key]. No-op if the key does not exist. */
    fun delete(key: String)

    /** Removes all entries managed by this storage. */
    fun clear()
}

// ---------------------------------------------------------------------------
// In-memory implementation — useful for unit tests
// ---------------------------------------------------------------------------

/**
 * Thread-unsafe in-memory [HydratedStorage] for use in unit tests.
 *
 * ```kotlin
 * val bloc = CounterBloc(storage = InMemoryStorage())
 * ```
 */
class InMemoryStorage : HydratedStorage {
    private val store = mutableMapOf<String, ByteArray>()

    override fun read(key: String): ByteArray? = store[key]
    override fun write(key: String, value: ByteArray) { store[key] = value }
    override fun delete(key: String) { store.remove(key) }
    override fun clear() = store.clear()
}
