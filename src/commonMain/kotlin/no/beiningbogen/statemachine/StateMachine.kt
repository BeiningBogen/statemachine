package no.beiningbogen.statemachine

import kotlinx.coroutines.flow.Flow

interface StateMachine<STATE, EVENT> {
    /**
     * The flow used to emit each new state.
     */
    val state: Flow<STATE>

    /**
     * Register a transition associated to an event name.
     * @param eventName: the name of the event that should trigger
     * a specific transition.
     * @param transition: the transition to register.
     */
    fun register(eventName: String, transition: Transition<STATE, EVENT>)

    /**
     * When triggered, the state machine will check if there are any registered
     * transition associated to that event and if it can be executed. If both
     * conditions are met, the state machine will execute the transition.
     * @param event: the event to use to trigger a transition.
     * @return the [STATE] resulting of the transition.
     * @throws : CannotApplyEventError when the event passed as parameter cannot be applied
     * to the current state in which the state machine is.
     */
    fun <T : EVENT> onEvent(eventName: String, event: T)

    /**
     * Clean up the supervisor job and the coroutine scope to prevent leaking resources.
     */
    fun destroy()
}
