# State Machine

A state machine dsl for kotlin multi-platform project

## Gradle

Inside the `repositories` block from your `build.gradle` file : 
```
maven { url = uri("https://maven.pkg.jetbrains.space/beiningbogen/p/stmn/maven") }
```
Inside `dependencies`
```
implementation "no.beiningbogen:StateMachine:0.3.0"
```

## Usage 

```
val stateMachine = StateMachine.create(State.Initial) {
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

val loadedState = stateMachine.onEvent(AppEvent.LoadData)
assertEquals(loadedState, stateMachine.state)
assertTrue(loadedState is AppState.Loaded<*>)
assertEquals(items, loadedState.data)
```

More details in StateMachineTest.kt
