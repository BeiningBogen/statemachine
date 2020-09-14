package no.beiningbogen.statemachine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
class DslTransitionRegistry {

    private val registry = mutableMapOf<DslTransitionMatcher, DslTransition<out Event>>()

    fun registerTransition(
        stateType: KClass<out State>,
        eventType: KClass<out Event>,
        transition: DslTransition<out Event>
    ) {
        registry[DslTransitionMatcher(stateType, eventType)] = transition
    }

    fun <T : Event> findTransition(
        otherStateType: KClass<out State>,
        otherEventType: KClass<out Event>
    ): DslTransition<T>? {
        val key = registry.keys.firstOrNull { it.matches(otherStateType, otherEventType) } ?: return null
        return registry[key] as DslTransition<T>
    }
}
