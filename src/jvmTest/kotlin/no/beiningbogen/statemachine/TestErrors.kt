package no.beiningbogen.statemachine

sealed class TestErrors {
    abstract val message: String

    object NetworkError : TestErrors() {
        override val message = "something went wrong, try again later"
    }
}