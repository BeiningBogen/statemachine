package no.beiningbogen.statemachine.error

import no.beiningbogen.statemachine.StateMachine

/**
 * [CannotRetryError] is used when the developer calls [StateMachine.retry] but the
 * latest state of the state machine is not Error.
 */
object CannotRetryError : Throwable() {
    override val message = "retry can only be executed when the state machine is on State.Error"
}
