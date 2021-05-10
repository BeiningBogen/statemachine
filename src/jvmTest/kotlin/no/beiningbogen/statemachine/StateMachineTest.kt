package no.beiningbogen.statemachine

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

data class User(
    val id: String,
    val name: String,
)

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

@ExperimentalTime
@ExperimentalCoroutinesApi
class StateMachineTest {

    private lateinit var stateMachine: StateMachine<TestState, TestEvent>
    private val dispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        val initialState = TestState()

        /**
         * Create and initialize a state machine with a builder lambda.
         */
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
                        // do something with the picture object here
                    }
                )
            }
        }
    }

    @After
    fun cleanUp() {
        stateMachine.destroy()
    }

    @Test
    fun shouldBeInitialState() = dispatcher.runBlockingTest {
        stateMachine.state.test {
            assertEquals(TestState(), expectItem())
        }
    }

    @Test
    fun shouldTransitionUserLoadedState() = dispatcher.runBlockingTest {
        stateMachine.state.test {
            var nextState = expectItem()
            assertEquals(TestState(), nextState)

            stateMachine.onEvent(TestEvent.LoadUser("1"))

            nextState = expectItem()
            assertTrue(nextState.isLoading)
            assertNull(nextState.user)
            assertNull(nextState.error)

            nextState = expectItem()
            assertFalse(nextState.isLoading)
            assertEquals(User("1", "John"), nextState.user)
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
