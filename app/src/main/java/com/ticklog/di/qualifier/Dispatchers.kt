package com.ticklog.di.qualifier

import javax.inject.Qualifier

/**
 * Qualifiers that let Hilt distinguish between the different coroutine
 * dispatchers it provides.
 *
 * Injecting dispatchers (rather than referencing `Dispatchers.IO` directly)
 * keeps classes testable — a test can supply a deterministic dispatcher — and
 * makes threading intent explicit at every call site.
 */

/** The dispatcher for disk/database/network-style blocking work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** The dispatcher for CPU-bound work (sorting, aggregation, parsing). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** The main/UI dispatcher. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
