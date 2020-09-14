package no.beiningbogen.statemachine

import kotlin.reflect.KClass

class DslTransitionMatcher(
    private val stateType: KClass<out State>,
    private val eventType: KClass<out Event>
) {

    fun matches(
        otherStateType: KClass<out State>,
        otherEventType: KClass<out Event>
    ): Boolean {
        return stateType == otherStateType && eventType == otherEventType
    }
}
