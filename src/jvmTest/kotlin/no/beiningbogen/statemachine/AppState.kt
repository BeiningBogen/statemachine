package no.beiningbogen.statemachine

sealed class AppState : State {
    override val isErrorState = false

    object Initial : AppState()

    object Loading : AppState()
    object AnotherState : AppState()
    data class Loaded<T>(val data: T) : AppState()

    sealed class Error : AppState() {
        override val isErrorState = true
        abstract val message: String

        object NetworkError : Error() {
            override val message = "something when wrong, try again later"
        }
    }
}
