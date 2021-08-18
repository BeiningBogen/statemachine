package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext

/**
 * A helper function to create a new state machine.
 * @param initialState: the initial state the machine should take.
 * @param coroutineContext: the context used for creating the state machine supervisor scope to run transitions.
 * @param builder: the lambda for registering transitions in the state machine.
 */
@ExperimentalCoroutinesApi
fun <STATE, EVENT> createStateMachine(
    initialState: STATE,
    coroutineContext: CoroutineContext,
    builder: StateMachine<STATE, EVENT>.() -> Unit
): StateMachine<STATE, EVENT> {
    return StateMachineImpl<STATE, EVENT>(initialState, coroutineContext)
        .apply { builder(this) }
}

/**
 * An extension function for registering a predefined transition without the need to pass its name.
 * @param transition: the transition to register.
 */
inline fun <STATE, EVENT, reified T : EVENT> StateMachine<STATE, EVENT>.register(transition: Transition<STATE, T>) {
    val eventName = T::class.toString()
    register(eventName, transition as Transition<STATE, EVENT>)
}

/**
 * An extension function for registering an anonymous transition without the need to pass its name.
 * @param builder: the lambda constructing the transition, see [transition].
 */
inline fun <STATE, EVENT, reified T : EVENT> StateMachine<STATE, EVENT>.register(builder: () -> Transition<STATE, T>) {
    val eventName = T::class.toString()
    register(eventName, builder() as Transition<STATE, EVENT>)
}

/**
 * A helper function returning an anonymous object implementing [Transition].
 * Use this function to avoid boilerplate and achieve a better readability.
 * @param predicate: the lambda used by the created transition for [Transition.isExecutable]
 * @param execution: the lambda used by the create transition for [Transition.execute]
 * @return a [Transition]
 */
fun <STATE, EVENT> transition(
    predicate: (STATE) -> Boolean,
    execution: suspend (MutableStateFlow<STATE>) -> Unit,
): Transition<STATE, EVENT> {
    return object : Transition<STATE, EVENT> {
        override val isExecutable: (STATE) -> Boolean = predicate
        override val execute: suspend (MutableStateFlow<STATE>) -> Unit = execution
    }
}


/**
 * An extension function for trigger a registered transition without the need to pass the event name.
 * @param builder: the transition to register.
 */
inline fun <STATE, EVENT, reified T : EVENT> StateMachine<STATE, EVENT>.onEvent(event: T) {
    val eventName = T::class.toString()
    onEvent(eventName, event)
}
