package no.beiningbogen.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
internal class StateMachineImpl<STATE, EVENT>(
    initialState: STATE,
    coroutineContext: CoroutineContext,
) : StateMachine<STATE, EVENT> {

    private val supervisor = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineContext + supervisor)
    private var runningJob: Job? = null

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<STATE> = _state

    internal val transitions = mutableMapOf<String, Transition<STATE, EVENT>>()

    override fun register(eventName: String, transition: Transition<STATE, EVENT>) {
        transitions[eventName] = transition
    }

    override fun <T : EVENT> onEvent(eventName: String, event: T) {
        val transition = findTransition(eventName)

        runningJob = coroutineScope.launch {
            if (transition.isExecutable(state.value)) {
                transition.execute(_state)
            }
        }
    }

    override fun destroy() {
        runningJob?.cancel()
        supervisor.cancel()
        coroutineScope.cancel()
    }
}

@ExperimentalCoroutinesApi
private fun <STATE, EVENT> StateMachineImpl<STATE, EVENT>.findTransition(
    eventName: String
): Transition<STATE, EVENT> {
    return transitions.filter { it.key == eventName }
        .map { it.value }
        .firstOrNull()
        ?: throw Error("event $eventName is not registered")
}
