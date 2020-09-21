package no.beiningbogen.statemachine

sealed class AppState : State() {
    object Loading : AppState()
    object AnotherState: AppState()
    data class Loaded<T>(val data: T) : AppState()
}
