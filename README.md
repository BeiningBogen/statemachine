# State Machine

A state machine dsl for kotlin multi-platform project.

StateMachine let you create and register transitions for a specific state object.
Each transition requires a predicate checking the current state of the machine to ensure it's ran at the correct time.

## Gradle

Inside the `repositories` block from your `build.gradle` file : 
```
maven { url = uri("https://maven.pkg.jetbrains.space/beiningbogen/p/stmn/maven") }
```
Inside `dependencies`
```
implementation "no.beiningbogen:StateMachine:1.0.1"
```

## Usage 

```
data class TestState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: TestError? = null,
)

sealed class TestEvent {
    data class LoadUser(val id: String) : TestEvent()
    data class DeleteUser(val id: String) : TestEvent()
}

class LoadUserName : Transition<TestState, TestEvent.LoadUser> {
    override val isExecutable: (TestState) -> Boolean = { !it.isLoading }
    override val execute: suspend TransitionUtils<TestState, TestEvent.LoadUser>.() -> Unit = {
        emitNewState(currentState().copy(isLoading = true))
        val user = loadUser(event.id)
        emitNewState(
            currentState().copy(
                isLoading = false,
                user = user
            )
        )
    }

    private fun loadUser(id: String): User {
        // do whatever here with the id
        return User(id = id, name = "John")
    }
}

stateMachine = createStateMachine(initialState, dispatcher) {
    /**
     * Register a predefined transition.
     */
    register(LoadUserName())

    /**
     * Register an anonymous transition here
     */
    register {
        transition<TestState, TestEvent.DeleteUser>(
            predicate = { !it.isLoading },
            execution = {
                // do something with DeleteUser.id here
            }
        )
    }
}
```

More details in StateMachineTest.kt
