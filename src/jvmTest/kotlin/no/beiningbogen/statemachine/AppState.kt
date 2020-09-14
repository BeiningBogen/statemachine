package no.beiningbogen.statemachine

sealed class AppState : State() {
    object Loading : AppState()
    data class Loaded<T>(val data: T) : AppState()
}
