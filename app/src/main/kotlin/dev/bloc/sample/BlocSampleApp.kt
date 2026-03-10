package dev.bloc.sample

import android.app.Application
import dev.bloc.BlocObserver
import dev.bloc.HydratedBloc
import dev.bloc.SharedPreferencesStorage

class BlocSampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        HydratedBloc.storage = SharedPreferencesStorage(this)
        BlocObserver.shared = AppBlocObserver()
    }

    companion object {
        var instance: BlocSampleApp? = null
            private set
    }
}
