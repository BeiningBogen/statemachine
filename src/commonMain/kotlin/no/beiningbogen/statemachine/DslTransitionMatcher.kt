package no.beiningbogen.statemachine

import kotlin.reflect.KClass

/**
 * DslTransitionMatcher is an object used as a key in [DslTransitionRegistry].
 * If we only store a list of [DslTransition], we have no way of finding a
 * specific transition later on. So the role of [DslTransitionMatcher] is to
 * tell if the [DslTransition] associated with it corresponds to a given
 * state/event pair.
 */

internal class DslTransitionMatcher(
    private val stateType: KClass<out State>,
    private val eventType: KClass<out Event>
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
        otherStateType: KClass<out State>,
        otherEventType: KClass<out Event>
    ): Boolean {
        return stateType == otherStateType && eventType == otherEventType
    }
}
