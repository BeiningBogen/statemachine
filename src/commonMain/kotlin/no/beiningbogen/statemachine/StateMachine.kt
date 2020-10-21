package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import no.beiningbogen.statemachine.error.CannotApplyEventError
import kotlin.reflect.KClass

/**
 * The main classes used by the developer to create a new state machine.
 */

@ExperimentalCoroutinesApi
class StateMachine<STATE : Any, EVENT : Any>(
    private val initialState: STATE,
    private val registry: DslTransitionRegistry<STATE, EVENT>
) {

    private val _state = mutableListOf<STATE>().apply {
        add(initialState)
    }

    /**
     * The current state of the state machine, it is set to
     * a default value when the state machine is created.
     * Setting this variable will make the state machine jump to the
     * specified state without any checks. It should be used only when
     * testing.
     */
    var state: STATE
        get() = _state[_state.lastIndex]
        set(value) {
            _state.add(value)
        }

    /**
     * Triggers a transition to a new state.
     * @param event: the event to use to trigger a transition.
     * @return the [STATE] resulting of the transition.
     */
    suspend fun <T : EVENT> onEvent(event: T): STATE {
        return onEvent(event, state::class)
    }

    /**
     * Call [onEvent] after a specified delay.
     * @param event: the event to use to trigger a transition.
     * @param delay: the delay to wait before retrying to apply the event.
     * @return the [STATE] resulting of the transition.
     */
    suspend fun <T : EVENT> retry(event: T, delay: Long = 0): STATE {
        delay(delay)
        return onEvent(event, _state[_state.lastIndex - 1]::class)
    }

    private suspend fun <T : EVENT> onEvent(event: T, stateType: KClass<out STATE>): STATE {
        val dslTransition = registry.findTransition<T>(stateType, event::class)

        val newState = dslTransition?.block?.invoke(event)
            ?: throw CannotApplyEventError(state, event)

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
        fun <STATE : Any, EVENT : Any> create(
            initialState: STATE,
            configuration: DslStateMachineBuilder<STATE, EVENT>.() -> Unit
        ): StateMachine<STATE, EVENT> {
            val builder = DslStateMachineBuilder<STATE, EVENT>()
            configuration(builder)
            return builder.build(initialState)
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
    val registry = DslTransitionRegistry<STATE, EVENT>()

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
     * state(SomeState, SomeOtherState) {
     *     ...
     * }
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
     * Build the state machine.
     */
    fun build(initialState: STATE): StateMachine<STATE, EVENT> {
        return StateMachine(initialState, registry)
    }
}

/**
 * Builder class to register different transitions.
 */

@ExperimentalCoroutinesApi
class DslStateBuilder<STATE : Any, EVENT : Any>(
    val stateType: KClass<out STATE>,
    val registry: DslTransitionRegistry<STATE, EVENT>
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
    inline fun <reified T : EVENT> on(noinline block: suspend (T) -> STATE) {
        val transition = DslTransition(block)
        registry.registerTransition(stateType, T::class, transition)
    }
}
