package no.beiningbogen.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import no.beiningbogen.statemachine.error.NoTransitionAssociated
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * The main classes used by the developer to create a new state machine.
 */
@ExperimentalCoroutinesApi
class StateMachine<STATE, EVENT : Any>(
    initialState: STATE,
    coroutineContext: CoroutineContext,
    private val registry: TransitionRegistry<STATE, EVENT>
) {
    private val supervisor = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineContext + supervisor)
    private var runningJob: Job? = null

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
    @Throws(NoTransitionAssociated::class)
    fun <T : EVENT> onEvent(event: T) {
        if (stateChannel.isClosedForSend) return

        val wrapper = registry.findTransitionWrapper(event::class)
            ?: throw NoTransitionAssociated(event)

        runningJob = coroutineScope.launch {
            wrapper.transition.invoke(TransitionUtils(stateChannel, event))
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
    @Throws(NoTransitionAssociated::class)
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
        runningJob?.cancel()
        supervisor.cancel()
        coroutineScope.cancel()
    }

    inner class TransitionUtils (
        private val sendChannel: SendChannel<STATE>,
        val event: EVENT,
    ) {
        val state: STATE
            get() = currentState

        suspend fun send(newState: STATE) {
            if (!sendChannel.isClosedForSend) {
                sendChannel.send(newState)
            }
        }

        fun offer(newState: STATE) {
            if (!sendChannel.isClosedForSend) {
                sendChannel.offer(newState)
            }
        }
    }

    companion object {

        /**
         * The starting point to create a new state machine.
         * @param configuration: a [StateMachineBuilder] to register states
         * on which events can be applied.
         */
        fun <STATE : Any, EVENT : Any> create(
            initialState: STATE,
            coroutineContext: CoroutineContext,
            configuration: StateMachineBuilder<STATE, EVENT>.() -> Unit
        ): StateMachine<STATE, EVENT> {
            val builder = StateMachineBuilder<STATE, EVENT>()
            configuration(builder)
            return builder.build(initialState, coroutineContext)
        }
    }
}

/**
 * Builder class to register the different states covered by the state machine.
 */
@ExperimentalCoroutinesApi
class StateMachineBuilder<STATE : Any, EVENT : Any> {

    /**
     * The registry containing all the different transitions
     * between states covered by the state machine.
     */
    val registry = TransitionRegistry<STATE, EVENT>()

    inline fun <reified T : EVENT> on(
        noinline lambda: Transition<STATE, T>
    ) {
        val transition = TransitionWrapper(lambda)
        registry.registerTransition(T::class, transition)
    }

    /**
     * Build the state machine.
     */
    fun build(initialState: STATE, coroutineContext: CoroutineContext): StateMachine<STATE, EVENT> {
        return StateMachine(initialState, coroutineContext, registry)
    }
}
