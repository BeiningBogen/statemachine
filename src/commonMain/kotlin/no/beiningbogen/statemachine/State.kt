package no.beiningbogen.statemachine

/**
 * Interface to be implemented by the developer's class representing the state of the a state machine.
 * This is use internally for checking if the current state of the state machine represent an error
 * state when the developer calls [StateMachine.retry].
 */
interface State {
    val isErrorState: Boolean
}
