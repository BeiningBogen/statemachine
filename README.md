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
data class UserProfileState(
    val isLoadingBasicInfo: Boolean = false,
    val isLoadingComplexInfo: Boolean = false,
    val name: String? = null,
    val profilePictureUrl: String? = null,
    val info: List<SomeComplexObject> = emptyList(),
)

val initialState = UserProfileState()

val stateMachine = StateMachine.create(initialState) {
    on<AppEvent.ShowLoading> { transitionUtils ->
        transitionUtils.send(
            transitionUtils.getCurrentState().copy(
                isLoadingBasicInfo = true,
                isLoadingComplexInfo = true,
            )
        )
    }

    ...
}

fun suspend someFunctionLater() {
    stateMachine.state.collect { userProfileState ->
        // updated state
    }
}
```

More details in StateMachineTest.kt
