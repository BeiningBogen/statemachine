package no.beiningbogen.statemachine

import kotlinx.coroutines.CoroutineScope

internal expect fun runBlockingTest(block: suspend CoroutineScope.() -> Unit)