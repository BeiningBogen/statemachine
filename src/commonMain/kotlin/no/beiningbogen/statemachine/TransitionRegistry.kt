package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

/**
 * Stores all the transitions declared by the developer in the state machine dsl.
 */

@ExperimentalCoroutinesApi
class TransitionRegistry<STATE, EVENT : Any> {

    /**
     * A map of [TransitionWrapper] associated with [TransitionMatcher] as its key.
     * It holds all the possible transition of a state machine created with the dsl.
     */
    private val registry = mutableMapOf<KClass<out EVENT>, TransitionWrapper<STATE, out EVENT>>()

    /**
     * Add a [TransitionWrapper] to the registry with a specific [TransitionMatcher]
     * @param stateType: The state on which the transition can be applied.
     * @param eventType: The event triggering the transition.
     * @param transition: the transition to use by the state machine to move on the next state.
     */
    fun registerTransition(
        eventType: KClass<out EVENT>,
        transition: TransitionWrapper<STATE, out EVENT>
    ) {
        registry[eventType] = transition
    }

    /**
     * Retrieves a matching [TransitionWrapper].
     * @param stateType: The state type to look for in the registry.
     * @param eventType: The event type to look for in the registry.
     * @return A [TransitionWrapper] if any of the one registered matches the
     * pair of state and event type passed as parameters, otherwise null.
     */
    internal fun findTransitionWrapper(
        eventType: KClass<out EVENT>
    ): TransitionWrapper<STATE, EVENT>? {
        val key = registry.keys.firstOrNull { it == eventType } ?: return null
        return registry[key] as TransitionWrapper<STATE, EVENT>
    }
}
