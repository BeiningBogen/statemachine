package no.beiningbogen.statemachine

class TransitionUtils<STATE, EVENT>(
    val currentState: () -> STATE,
    val event: EVENT,
    val emitNewState: suspend (STATE) -> Unit
)

interface Transition<STATE, EVENT> {
    /**
     * A lambda used to determine if the transition should be executed or not.
     * It has a state parameter as input, it should be the current state of the
     * state machine. A typical use case would be to check if data has already
     * been loaded before executing the transition if its purpose is to load data.
     */
    val isExecutable: (STATE) -> Boolean

    /**
     * A lambda executing the logic to transition the state machine from it's
     * current state to the next one.
     */
    val execute: suspend TransitionUtils<STATE, EVENT>.() -> Unit
}
