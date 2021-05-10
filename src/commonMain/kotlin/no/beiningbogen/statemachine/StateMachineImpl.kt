package no.beiningbogen.statemachine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
internal class StateMachineImpl<STATE, EVENT>(
    initialState: STATE,
    coroutineContext: CoroutineContext,
) : StateMachine<STATE, EVENT> {

    private val supervisor = SupervisorJob()
    private val coroutineScope = CoroutineScope(coroutineContext + supervisor)
    private var runningJob: Job? = null

    private var currentState: STATE = initialState
    private val stateChannel = Channel<STATE>()

    internal val transitions = mutableMapOf<String, Transition<STATE, EVENT>>()

    override val state: Flow<STATE> = stateChannel.receiveAsFlow()
        .onStart { emit(initialState) }
        .onEach { currentState = it }

    override fun register(eventName: String, transition: Transition<STATE, EVENT>) {
        transitions[eventName] = transition
    }

    override fun <T : EVENT> onEvent(eventName: String, event: T) {
        if (stateChannel.isClosedForSend) return
        val transition = findTransition(eventName)

        runningJob = coroutineScope.launch {
            if (transition.isExecutable(currentState)) {
                val transitionUtils = TransitionUtils<STATE, EVENT>(
                    currentState = { currentState },
                    event = event,
                    emitNewState = { stateChannel.send(it) }
                )
                transition.execute(transitionUtils)
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
