package no.beiningbogen.statemachine

sealed class TestStates {

    object Initial : TestStates()
    object Loading : TestStates()
    data class Loaded<T>(val data: T) : TestStates()
    data class Error(val message: String): TestStates()
}
