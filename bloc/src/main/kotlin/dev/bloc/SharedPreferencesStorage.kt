package dev.bloc

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

/**
 * [HydratedStorage] backed by [SharedPreferences].
 *
 * State bytes are Base64-encoded before writing so they survive the string-only
 * SharedPreferences API without corruption.
 *
 * Create once and assign to [HydratedBloc.storage] at app startup, before any
 * HydratedBlocs are instantiated:
 *
 * ```kotlin
 * class App : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         HydratedBloc.storage = SharedPreferencesStorage(this)
 *         BlocObserver.shared = AppBlocObserver()
 *     }
 * }
 * ```
 *
 * The SharedPreferences file is named `"dev.bloc.hydrated"` by default.
 * Override [prefsName] to use a custom file name (e.g. for migration or testing).
 */
class SharedPreferencesStorage(
    context: Context,
    private val prefsName: String = "dev.bloc.hydrated",
) : HydratedStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun read(key: String): ByteArray? {
        val encoded = prefs.getString(key, null) ?: return null
        return Base64.decode(encoded, Base64.DEFAULT)
    }

    override fun write(key: String, value: ByteArray) {
        prefs.edit()
            .putString(key, Base64.encodeToString(value, Base64.DEFAULT))
            .apply()
    }

    override fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
