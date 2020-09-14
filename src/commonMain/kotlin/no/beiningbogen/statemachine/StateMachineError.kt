package no.beiningbogen.statemachine

abstract class StateMachineError {
    abstract val message: String

    data class CannotApplyEvent(val state: State, val event: Event) : StateMachineError() {
        override val message = "event $event cannot be applied to state $state"
    }

    object CannotRetry : StateMachineError() {
        override val message = "retry can only be executed when the state machine is on State.Error"
    }
}
