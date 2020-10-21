package no.beiningbogen.statemachine

/**
 * A simple object holding the lambda creating a new state.
 * This class should not be use at any point by devs to create a
 * state machine. It is only useful internally.
 */

class DslTransition<out STATE, EVENT : Any>(

    /**
     * The lambda creating a new [STATE] object based on the [EVENT] parameter.
     */
    val block: suspend (EVENT) -> STATE
)
