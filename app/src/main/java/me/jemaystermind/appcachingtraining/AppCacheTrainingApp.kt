package me.jemaystermind.appcachingtraining

import android.app.Application
import timber.log.Timber

class AppCacheTrainingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}