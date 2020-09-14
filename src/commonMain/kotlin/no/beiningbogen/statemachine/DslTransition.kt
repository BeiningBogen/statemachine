package no.beiningbogen.statemachine

class DslTransition<U : Event>(
    val block: suspend (U) -> State
)
