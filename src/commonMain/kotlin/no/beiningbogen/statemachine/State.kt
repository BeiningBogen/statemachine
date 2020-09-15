package no.beiningbogen.statemachine

/**
 * Base class for any states the state machine should cover.
 * It must be extended in clients, for example :
 *
 * sealed class AppState : State() {
 *     object Loading : AppState()
 *     object Loaded : AppState()
 * }
 */

abstract class State {

    /**
     * The default state for any state machine created with the dsl.
     */
    object Initial : State()

    /**
     * The state used when something wrong occurred with the state machine.
     */
    data class Error(val error: StateMachineError) : State()
}
