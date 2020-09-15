package no.beiningbogen.statemachine

/**
 * Base class for any events the state machine should cover.
 * It must be extended in clients, for example :
 *
 * sealed class AppEvent : Event() {
 *     object ShowLoading : AppEvent()
 *     object LoadData : AppEvent()
 * }
 */

abstract class Event
