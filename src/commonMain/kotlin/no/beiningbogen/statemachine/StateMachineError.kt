package no.beiningbogen.statemachine

/**
 * Error specific to internal logic of the state machine library.
 */

abstract class StateMachineError {
    abstract val message: String

    /**
     * [CannotApplyEvent] Is used when the developer calls [StateMachine.onEvent]
     * with an event that was not registered for a specific state. This means
     * there is no transition for this situation.
     */
    data class CannotApplyEvent(val state: State, val event: Event) : StateMachineError() {
        override val message = "event $event cannot be applied to state $state"
    }

    /**
     * [CannotRetry] is used when the developer calls [StateMachine.retry] but the
     * latest state of the state machine is not Error
     */
    object CannotRetry : StateMachineError() {
        override val message = "retry can only be executed when the state machine is on State.Error"
    }
}
