package no.beiningbogen.statemachine

sealed class AppError : StateMachineError() {
    object NetworkError: AppError() {
        override val message = "something when wrong, try again later"
    }
}