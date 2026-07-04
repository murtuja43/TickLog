package com.ticklog

import android.app.Application
import com.ticklog.ui.feature_widget.WidgetSynchronizer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation and create
 * the application-level dependency container that every other injected class is
 * ultimately rooted in.
 *
 * Its one active responsibility is to start the [WidgetSynchronizer], which
 * keeps any home-screen widgets reflecting the latest checklist state while the
 * app is in the foreground.
 */
@HiltAndroidApp
class TickLogApplication : Application() {

    @Inject
    lateinit var widgetSynchronizer: WidgetSynchronizer

    override fun onCreate() {
        super.onCreate()
        widgetSynchronizer.start(this)
    }
}
