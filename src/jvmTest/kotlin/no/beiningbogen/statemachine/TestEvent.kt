package no.beiningbogen.statemachine

sealed class TestEvent {
    object ShowLoading : TestEvent()
    object LoadUser : TestEvent()
    data class LoadPictures(val offset: Int, val limit: Int) : TestEvent()
    data class LoadVideos(val offset: Int, val limit: Int) : TestEvent()
}
