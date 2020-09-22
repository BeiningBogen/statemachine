package no.beiningbogen.statemachine

sealed class AppEvent {
    object ShowLoading : AppEvent()
    object LoadData : AppEvent()
    data class SearchItemByName(val name: String, val page: Int) : AppEvent()
}
