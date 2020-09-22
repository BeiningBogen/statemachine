package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

/**
 * Stores all the transitions declared by the developer in the state machine dsl.
 */

@ExperimentalCoroutinesApi
class DslTransitionRegistry<STATE : State, EVENT : Any> {

    /**
     * A map of [DslTransition] associated with [DslTransitionMatcher] as its key.
     * It holds all the possible transition of a state machine created with the dsl.
     */
    private val registry = mutableMapOf<DslTransitionMatcher<STATE, EVENT>, DslTransition<STATE, out EVENT>>()

    /**
     * Add a [DslTransition] to the registry with a specific [DslTransitionMatcher]
     * @param stateType: The state on which the transition can be applied.
     * @param eventType: The event triggering the transition.
     * @param transition: the transition to use by the state machine to move on the next state.
     */
    fun registerTransition(
        stateType: KClass<out STATE>,
        eventType: KClass<out EVENT>,
        transition: DslTransition<STATE, out EVENT>
    ) {
        registry[DslTransitionMatcher(stateType, eventType)] = transition
    }

    /**
     * Retrieves a matching [DslTransition].
     * @param stateType: The state type to look for in the registry.
     * @param eventType: The event type to look for in the registry.
     * @return A [DslTransition] if any of the one registered matches the
     * pair of state and event type passed as parameters, otherwise null.
     */
    internal fun <T : EVENT> findTransition(
        stateType: KClass<out STATE>,
        eventType: KClass<out EVENT>
    ): DslTransition<STATE, EVENT>? {
        val key = registry.keys.firstOrNull { it.matches(stateType, eventType) } ?: return null
        return registry[key] as DslTransition<STATE, EVENT>
    }
}
