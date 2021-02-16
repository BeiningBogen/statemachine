package no.beiningbogen.statemachine

import kotlinx.coroutines.channels.SendChannel

/**
 * A simple object holding the lambda creating a new state.
 * This class should not be use at any point by devs to create a
 * state machine. It is only useful internally.
 */

class TransitionWrapper<STATE, EVENT : Any>(

    /**
     * The lambda creating a new [STATE] object based on the [EVENT] parameter.
     */
    val transition: suspend (EVENT, SendChannel<STATE>) -> Unit
)
