package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

/**
 * Stores all the transitions declared by the developer in the state machine dsl.
 */

@ExperimentalCoroutinesApi
class DslTransitionRegistry {

    /**
     * The map containing [DslTransition] associated with [DslTransitionMatcher] as a key.
     * It holds all the possible transition of a state machine created with the dsl.
     */
    private val registry = mutableMapOf<DslTransitionMatcher, DslTransition<out Event>>()

    /**
     * Add [DslTransition] to the registry with a specific [DslTransitionMatcher]
     * @param stateType: The state on which the transition can be applied.
     * @param eventType: The event triggering the transition.
     * @param transition: the transition to use by the state machine to move on the next state.
     */
    fun registerTransition(
        stateType: KClass<out State>,
        eventType: KClass<out Event>,
        transition: DslTransition<out Event>
    ) {
        registry[DslTransitionMatcher(stateType, eventType)] = transition
    }

    /**
     * Retrieves a matching [DslTransition].
     * @param otherStateType: The state type to look for in the registry.
     * @param otherEventType: The event type to look for in the registry.
     * @return A [DslTransition] if any of the one registered matches the
     * pair of state and event type passed as parameters, otherwise null.
     */
    internal fun <T : Event> findTransition(
        otherStateType: KClass<out State>,
        otherEventType: KClass<out Event>
    ): DslTransition<T>? {
        val key = registry.keys.firstOrNull { it.matches(otherStateType, otherEventType) } ?: return null
        return registry[key] as DslTransition<T>
    }
}
