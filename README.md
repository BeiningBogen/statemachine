# State Machine

A state machine dsl for kotlin multi-platform project

## Usage 

```
val stateMachine = StateMachine.create {
    state<State.Initial> {
        on<AppEvent.ShowLoading> {
            AppState.Loading
        }
    }

    state<AppState.Loading> {
        on<AppEvent.LoadData> {
            AppState.Loaded(items)
        }
    }
}

assertEquals(State.Initial, stateMachine.state)

val loadState = stateMachine.onEvent(AppEvent.ShowLoading)
assertEquals(AppState.Loading, loadState)
assertEquals(AppState.Loading, stateMachine.state)

val loadedState = stateMachine.onEvent(AppEvent.LoadData)
assertEquals(loadedState, stateMachine.state)
assertTrue(loadedState is AppState.Loaded<*>)
assertEquals(items, loadedState.data)
```

More details in StateMachineTest.kt