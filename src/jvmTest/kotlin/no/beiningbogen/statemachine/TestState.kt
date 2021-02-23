package no.beiningbogen.statemachine

data class TestState(
    val isUserLoading: Boolean = false,
    val arePicturesLoading: Boolean = false,
    val user: String? = null,
    val pictures: List<String> = emptyList(),
    val error: TestError? = null,
)
