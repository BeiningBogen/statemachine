package no.beiningbogen.statemachine

abstract class State {
    object Initial : State()
    data class Error(val error: StateMachineError) : State()
}
