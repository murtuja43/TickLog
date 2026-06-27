package com.ticklog

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation and create
 * the application-level dependency container that every other injected class is
 * ultimately rooted in. No manual setup is required here — keeping the class
 * empty is intentional and idiomatic.
 */
@HiltAndroidApp
class TickLogApplication : Application()
