package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext

/**
 * A helper function to create a new state machine.
 * @param initialState: the initial state the machine should take.
 * @param coroutineContext: the context used for creating the state machine supervisor scope to run transitions.
 * @param builder: the lambda for registering transitions in the state machine.
 */
@ExperimentalCoroutinesApi
fun <STATE, EVENT, SIDE_EFFECT> createStateMachine(
    initialState: STATE,
    coroutineContext: CoroutineContext,
    builder: StateMachine<STATE, EVENT, SIDE_EFFECT>.() -> Unit
): StateMachine<STATE, EVENT, SIDE_EFFECT> {
    return StateMachineImpl<STATE, EVENT, SIDE_EFFECT>(initialState, coroutineContext)
        .apply { builder(this) }
}

/**
 * An extension function for registering a predefined transition without the need to pass its name.
 * @param transition: the transition to register.
 */
inline fun <STATE, EVENT, reified T : EVENT, SIDE_EFFECT> StateMachine<STATE, EVENT, SIDE_EFFECT>.register(transition: Transition<STATE, T>) {
    val eventName = T::class.toString()
    register(eventName, transition as Transition<STATE, EVENT>)
}

/**
 * An extension function for registering a predefined side effect transition without the need to pass its name.
 * @param transition: the transition to register.
 */
inline fun <STATE, EVENT, reified T : EVENT, SIDE_EFFECT> StateMachine<STATE, EVENT, SIDE_EFFECT>.register(transition: SideEffectTransition<STATE, T, SIDE_EFFECT>) {
    val eventName = T::class.toString()
    register(eventName, transition as SideEffectTransition<STATE, EVENT, SIDE_EFFECT>)
}

/**
 * An extension function for registering an anonymous transition without the need to pass its name.
 * @param builder: the lambda constructing the transition, see [transition].
 */
inline fun <STATE, EVENT, reified T : EVENT, SIDE_EFFECT> StateMachine<STATE, EVENT, SIDE_EFFECT>.registerTransition(builder: () -> Transition<STATE, T>) {
    val eventName = T::class.toString()
    register(eventName, builder() as Transition<STATE, EVENT>)
}

/**
 * An extension function for registering an anonymous side effect transition without the need to pass its name.
 * @param builder: the lambda constructing the transition, see [transition].
 */
inline fun <STATE, EVENT, reified T : EVENT, SIDE_EFFECT> StateMachine<STATE, EVENT, SIDE_EFFECT>.registerSideEffect(builder: () -> SideEffectTransition<STATE, T, SIDE_EFFECT>) {
    val eventName = T::class.toString()
    register(eventName, builder() as SideEffectTransition<STATE, EVENT, SIDE_EFFECT>)
}

/**
 * A helper function returning an anonymous object implementing [Transition].
 * Use this function to avoid boilerplate and achieve a better readability.
 * @param predicate: the lambda used by the created transition for [Transition.isExecutable]
 * @param execution: the lambda used by the created transition for [Transition.execute]
 * @return a [Transition]
 */
fun <STATE, EVENT> transition(
    predicate: (STATE) -> Boolean,
    execution: suspend (EVENT, MutableStateFlow<STATE>) -> Unit,
): Transition<STATE, EVENT> {
    return object : Transition<STATE, EVENT> {
        override val isExecutable: (STATE) -> Boolean = predicate
        override val execute: suspend (EVENT, MutableStateFlow<STATE>) -> Unit = execution
    }
}

/**
 * A helper function returning an anonymous object implementing [SideEffectTransition].
 * Use this function to avoid boilerplate and achieve a better readability.
 * @param predicate: the lambda used by the created transition for [SideEffectTransition.isExecutable]
 * @param execution: the lambda used by the created transition for [SideEffectTransition.execute]
 * @return a [SideEffectTransition]
 */
fun <STATE, EVENT, SIDE_EFFECT> sideEffectTransition(
    predicate: (STATE) -> Boolean,
    execution: suspend (EVENT, MutableSharedFlow<SIDE_EFFECT>) -> Unit,
): SideEffectTransition<STATE, EVENT, SIDE_EFFECT> {
    return object : SideEffectTransition<STATE, EVENT, SIDE_EFFECT> {
        override val isExecutable: (STATE) -> Boolean = predicate
        override val execute: suspend (EVENT, MutableSharedFlow<SIDE_EFFECT>) -> Unit = execution
    }
}

/**
 * An extension function passing an event as an input for the state machine.
 * @param event: the event acting as an input for the state machine.
 */
inline fun <STATE, EVENT, reified T : EVENT, SIDE_EFFECT> StateMachine<STATE, EVENT, SIDE_EFFECT>.onEvent(event: T) {
    val eventName = T::class.toString()
    onEvent(eventName, event)
}
