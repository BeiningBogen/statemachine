package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
class StateMachine(private val registry: DslTransitionRegistry) {

    private val _state = mutableListOf<State>().apply {
        add(State.Initial)
    }
    var state: State
        get() = _state[_state.lastIndex]
        set(value) {
            _state.add(value)
        }

    suspend fun <T : Event> onEvent(event: T): State {
        return onEvent(event, state::class)
    }

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
        fun create(configuration: DslStateMachineBuilder.() -> Unit): StateMachine {
            val builder = DslStateMachineBuilder()
            configuration(builder)
            return builder.build()
        }
    }
}

@ExperimentalCoroutinesApi
class DslStateMachineBuilder {

    val registry = DslTransitionRegistry()

    inline fun <reified T : State> state(block: DslStateBuilder.() -> Unit) {
        val builder = DslStateBuilder(T::class, registry)
        block(builder)
    }

    fun build(): StateMachine {
        return StateMachine(registry)
    }
}

@ExperimentalCoroutinesApi
class DslStateBuilder(
    val stateType: KClass<out State>,
    val registry: DslTransitionRegistry
) {

    inline fun <reified U : Event> on(noinline block: suspend (U) -> State) {
        val transition = DslTransition(block)
        registry.registerTransition(stateType, U::class, transition)
    }
}
