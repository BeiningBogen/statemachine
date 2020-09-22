package no.beiningbogen.statemachine.error

import no.beiningbogen.statemachine.StateMachine

/**
 * [CannotApplyEventError] Is used when the developer calls [StateMachine.onEvent]
 * with an event that was not registered for a specific state. This means
 * there is no transition for this situation.
 */
class CannotApplyEventError(state: Any, event: Any) : Throwable() {
    override val message = "event ${event::class.simpleName} cannot be applied to state ${state::class.simpleName}"
}
