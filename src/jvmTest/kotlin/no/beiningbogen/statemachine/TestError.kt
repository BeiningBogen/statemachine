package no.beiningbogen.statemachine

sealed class TestError {
    abstract val message: String

    object NetworkError : TestError() {
        override val message = "something went wrong, try again later"
    }
}