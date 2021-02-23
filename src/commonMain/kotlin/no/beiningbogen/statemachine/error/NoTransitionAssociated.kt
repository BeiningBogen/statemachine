package no.beiningbogen.statemachine.error

import no.beiningbogen.statemachine.StateMachine

/**
 * [NoTransitionAssociated] Is used when the developer calls [StateMachine.onEvent]
 * with an event that was not registered. This means there is no transition for
 * this situation.
 */
class NoTransitionAssociated(event: Any) : Throwable() {
    override val message = "event ${event::class.simpleName} is not registered"
}
