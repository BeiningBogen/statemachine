package no.beiningbogen.statemachine

/**
 * A simple object holding the lambda creating a new state.
 * This class should not be use at any point by devs to create a
 * state machine. It is useful only internally.
 */

class DslTransition<U : Event>(

    /**
     * Lambda creating a State object.
     * it takes a parameter extending [Event].
     */
    val block: suspend (U) -> State
)
