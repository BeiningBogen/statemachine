package no.beiningbogen.statemachine

import kotlin.reflect.KClass

/**
 * DslTransitionMatcher is an object used as a key in [TransitionRegistry].
 * If we only store a list of [TransitionWrapper], we have no way of finding a
 * specific transition later on. So the role of [TransitionMatcher] is to
 * tell if the [TransitionWrapper] associated with it corresponds to a given
 * [STATE]/[EVENT] pair.
 */

internal class TransitionMatcher<STATE : Any, EVENT : Any>(
    private val stateType: KClass<out STATE>,
    private val eventType: KClass<out EVENT>
) {

    /**
     * Returns true if the state and event types passed as parameters
     * match the state an event types used to create this matcher.
     * @param otherStateType: the state type to be compared with the
     * matcher's state type.
     * @param otherEventType: the event type to be compared with the
     * matcher's event type.
     */
    internal fun matches(
        otherStateType: KClass<out STATE>,
        otherEventType: KClass<out EVENT>
    ): Boolean {
        return stateType == otherStateType && eventType == otherEventType
    }
}