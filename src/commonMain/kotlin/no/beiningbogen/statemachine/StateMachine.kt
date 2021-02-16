package no.beiningbogen.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import no.beiningbogen.statemachine.error.CannotApplyEventError
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * The main classes used by the developer to create a new state machine.
 */
@ExperimentalCoroutinesApi
class StateMachine<STATE : Any, EVENT : Any>(
    initialState: STATE,
    coroutineContext: CoroutineContext,
    private val registry: TransitionRegistry<STATE, EVENT>
) {
    private val supervisor = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineContext + supervisor)

    private var currentState: STATE = initialState
    private val stateChannel = Channel<STATE>()

    /**
     * The flow used to emit each new state the state machine takes.
     * It should be used along with shareIn/StateIn.
     */
    val state: Flow<STATE> = stateChannel.receiveAsFlow()
        .onStart { emit(initialState) }
        .onEach { currentState = it }

    /**
     * Triggers a transition to a new state.
     * @param event: the event to use to trigger a transition.
     * @return the [STATE] resulting of the transition.
     * @throws : CannotApplyEventError when the event passed as parameter cannot be applied
     * to the current state in which the state machine is.
     */
    @Throws(CannotApplyEventError::class)
    fun <T : EVENT> onEvent(event: T) {
        if (stateChannel.isClosedForSend) return

        val wrapper = registry.findTransitionWrapper(currentState::class, event::class)
            ?: throw CannotApplyEventError(currentState, event)

        coroutineScope.launch {
            wrapper.transition.invoke(event, stateChannel)
        }
    }

    /**
     * Call [onEvent] after a specified delay.
     * @param event: the event to use to trigger a transition.
     * @param delay: the delay to wait before retrying to apply the event.
     * @return the [STATE] resulting of the transition.
     * @throws : CannotApplyEventError when the event passed as parameter cannot be applied
     * to the current state in which the state machine is.
     */
    @Throws(CannotApplyEventError::class)
    fun <T : EVENT> retry(event: T, delay: Long = 0) {
        CoroutineScope(Dispatchers.Unconfined).launch {
            delay(delay)
            onEvent(event)
        }
    }

    /**
     * Clean up the supervisor job and the coroutine scope to prevent leaking resources.
     */
    fun destroy() {
        supervisor.cancel()
        coroutineScope.cancel()
    }

    companion object {

        /**
         * The starting point to create a new state machine.
         * @param configuration: a [DslStateMachineBuilder] to register states
         * on which events can be applied.
         */
        fun <STATE : Any, EVENT : Any> create(
            initialState: STATE,
            coroutineContext: CoroutineContext,
            configuration: DslStateMachineBuilder<STATE, EVENT>.() -> Unit
        ): StateMachine<STATE, EVENT> {
            val builder = DslStateMachineBuilder<STATE, EVENT>()
            configuration(builder)
            return builder.build(initialState, coroutineContext)
        }
    }
}

/**
 * Builder class to register the different states covered by the state machine.
 */
@ExperimentalCoroutinesApi
class DslStateMachineBuilder<STATE : Any, EVENT : Any> {

    /**
     * The registry containing all the different transitions
     * between states covered by the state machine.
     */
    val registry = TransitionRegistry<STATE, EVENT>()

    /**
     * Define the state on which the transitions declared in the lambda passed as parameter should
     * be applied on. For example :
     *
     * state<SomeState> {
     *     ...
     * }
     *
     * @param T : The given [STATE] to use in the lambda parameter.
     * @param block : the lambda defining transitions for the state machine.
     */
    inline fun <reified T : STATE> state(block: DslStateBuilder<STATE, EVENT>.() -> Unit) {
        val builder = DslStateBuilder(T::class, registry)
        block(builder)
    }

    /**
     * Define the states on which the transitions declared in the lambda passed as parameter should
     * be applied on. For example :
     *
     * states(SomeState, SomeOtherState) {
     *     ...
     * }
     *
     * This methode will work for states defined as object since they don't require
     * parameters. For declaring multiple states requiring parameters, see [states]
     *
     * @param states : an array/vararg of [STATE] on which the transitions will operate.
     * @param block : the lambda defining transitions for the state machine.
     */
    fun states(vararg states: STATE, block: DslStateBuilder<STATE, EVENT>.() -> Unit) {
        for (state in states) {
            val builder = DslStateBuilder(state::class, registry)
            block(builder)
        }
    }

    /**
     * Define the states on which the transitions declared in the lambda passed as parameter should
     * be applied on. For example :
     *
     * states(SomeState, SomeOtherState) {
     *     ...
     * }
     *
     * This methode will work for states defined as object or data classes.
     *
     * @param states : an array/vararg of [STATE] on which the transitions will operate.
     * @param block : the lambda defining transitions for the state machine.
     */
    fun states(vararg states: KClass<out STATE>, block: DslStateBuilder<STATE, EVENT>.() -> Unit) {
        for (state in states) {
            val builder = DslStateBuilder(state, registry)
            block(builder)
        }
    }

    /**
     * Build the state machine.
     */
    fun build(initialState: STATE, coroutineContext: CoroutineContext): StateMachine<STATE, EVENT> {
        return StateMachine(initialState, coroutineContext, registry)
    }
}

/**
 * Builder class to register different transitions.
 */

class DslStateBuilder<STATE : Any, EVENT : Any>(
    val stateType: KClass<out STATE>,
    val registry: TransitionRegistry<STATE, EVENT>
) {

    /**
     * Define the event triggering the lambda used for transitioning the
     * state machine to a new state. For example :
     *
     * state<SomeState> {
     *     on<SomeEvent> {
     *         SomeOtherState
     *     }
     * }
     *
     * @param T : The given [STATE] to use in the lambda parameter.
     * @param block : the lambda defining a specific transition.
     */
    inline fun <reified T : EVENT> on(noinline block: suspend (T, SendChannel<STATE>) -> Unit) {
        val transition = TransitionWrapper(block)
        registry.registerTransition(stateType, T::class, transition)
    }
}
