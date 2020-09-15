package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

/**
 * The main classes used by the clients to create a new state machine.
 */

@ExperimentalCoroutinesApi
class StateMachine(private val registry: DslTransitionRegistry) {

    private val _state = mutableListOf<State>().apply {
        add(State.Initial)
    }

    /**
     * The current state of the state machine, [State.Initial] is set
     * by default when the state machine is created.
     * Setting this variable will make the state machine jump to the
     * specified state without any checks. It should be used only when
     * testing.
     */
    var state: State
        get() = _state[_state.lastIndex]
        set(value) {
            _state.add(value)
        }

    /**
     * Triggers a transition to a new state.
     * @param event: the event to use to trigger a transition.
     * @return the [State] resulting of the transition.
     */
    suspend fun <T : Event> onEvent(event: T): State {
        return onEvent(event, state::class)
    }

    /**
     * Retry to apply an event with an eventual delay.
     * It can be used only if the current state of the state machine
     * extends [State.Error]
     * @param event: the event to use to trigger a transition.
     * @param delay: the delay to wait before retrying to apply the event.
     * @return the [State] resulting of the transition.
     */
    suspend fun <T : Event> retry(event: T, delay: Long = 0): State {
        if (state !is State.Error) return State.Error(StateMachineError.CannotRetry)

        delay(delay)
        return onEvent(event, _state[_state.lastIndex - 1]::class)
    }

    private suspend fun <T : Event> onEvent(event: T, stateType: KClass<out State>): State {
        val dslTransition = registry.findTransition<T>(stateType, event::class)

        val newState = dslTransition?.block?.invoke(event)
            ?: State.Error(StateMachineError.CannotApplyEvent(state, event))

        state = newState

        return newState
    }

    @ExperimentalCoroutinesApi
    companion object {

        /**
         * The starting point to create a new state machine.
         * @param configuration: a [DslStateMachineBuilder] to register states
         * on which events can be applied.
         */
        fun create(configuration: DslStateMachineBuilder.() -> Unit): StateMachine {
            val builder = DslStateMachineBuilder()
            configuration(builder)
            return builder.build()
        }
    }
}

/**
 * Builder class to register the different states the state machine covers.
 */

@ExperimentalCoroutinesApi
class DslStateMachineBuilder {

    /**
     * The registry containing all the different transitions
     * between states covered by the state machine.
     */
    val registry = DslTransitionRegistry()

    /**
     * Define the state on which the event declared in the lambda parameter should
     * be applied on. For example :
     *
     * state<State.SomeState> {
     *     ...
     * }
     *
     * @param T : The given State to use in the lambda parameter.
     * @param block : the lambda defining which event(s) that can be applied
     * on the given state.
     */
    inline fun <reified T : State> state(block: DslStateBuilder.() -> Unit) {
        val builder = DslStateBuilder(T::class, registry)
        block(builder)
    }

    /**
     * Build the state machine.
     */
    fun build(): StateMachine {
        return StateMachine(registry)
    }
}

/**
 * Builder class to register different events that can be applied to a given state.
 */

@ExperimentalCoroutinesApi
class DslStateBuilder(
    val stateType: KClass<out State>,
    val registry: DslTransitionRegistry
) {

    /**
     * Define the event triggering the lambda used for transitioning the
     * state machine to a new state. For example :
     *
     * state<State.SomeState> {
     *     on<Event.SomeEvent< {
     *         State.SomeOtherState
     *     }
     * }
     *
     * @param T : The given State to use in the lambda parameter.
     * @param block : the lambda defining which event(s) that can be applied
     * on the given state.
     */
    inline fun <reified U : Event> on(noinline block: suspend (U) -> State) {
        val transition = DslTransition(block)
        registry.registerTransition(stateType, U::class, transition)
    }
}
