package no.beiningbogen.statemachine

import app.cash.turbine.test
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@ExperimentalTime
@ExperimentalCoroutinesApi
class StateMachineTest {

    private lateinit var stateMachine: StateMachine<TestState, TestEvent>
    private lateinit var itemRepository: FakeItemRepository
    private val dispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        itemRepository = mock()
        val initialState = TestState()

        /**
         * Create and initialize the state machine to use in our tests.
         */
        stateMachine = StateMachine.create(initialState, dispatcher) {
            /**
             * Register a lambda triggered when [TestEvent.ShowLoading] will be passed to
             * [StateMachine.onEvent].
             */
            on<TestEvent.ShowLoading> {
                send(
                    state.copy(
                        isUserLoading = true,
                        arePicturesLoading = true,
                    )
                )
            }

            /**
             * Register a lambda triggered when [TestEvent.LoadUser] will be passed to
             * [StateMachine.onEvent].
             */
            on<TestEvent.LoadUser> {
                send(
                    state.copy(
                        isUserLoading = false,
                        user = "John"
                    )
                )
            }

            /**
             * Register a lambda triggered when [TestEvent.LoadPictures] will be passed to
             * [StateMachine.onEvent].
             */
            on<TestEvent.LoadPictures> {
                try {
                    val data = itemRepository.loadPictures(event.offset, event.limit)
                    send(
                        state.copy(
                            arePicturesLoading = false,
                            pictures = data
                        )
                    )
                } catch (e: Exception) {
                    send(
                        state.copy(
                            arePicturesLoading = false,
                            error = TestError.NetworkError
                        )
                    )
                }
            }
        }
    }

    @After
    fun cleanUp() {
        stateMachine.destroy()
    }

    @Test
    fun shouldBeStateInitial() = dispatcher.runBlockingTest {
        stateMachine.state.test {
            assertEquals(TestState(), expectItem())
        }
    }

    @Test
    fun shouldTransitionToStateLoading() = dispatcher.runBlockingTest {
        stateMachine.state.test {
            var nextState = expectItem()
            assertEquals(TestState(), nextState)

            stateMachine.onEvent(TestEvent.ShowLoading)

            nextState = expectItem()
            assertTrue(nextState.isUserLoading)
            assertTrue(nextState.arePicturesLoading)
            assertNull(nextState.user)
            assertTrue(nextState.pictures.isEmpty())
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shouldTransitionAfterTestEventLoadUser() = dispatcher.runBlockingTest {
        stateMachine.state.test {
            var nextState = expectItem()
            assertEquals(TestState(), nextState)

            stateMachine.onEvent(TestEvent.ShowLoading)

            nextState = expectItem()
            assertTrue(nextState.isUserLoading)
            assertTrue(nextState.arePicturesLoading)
            assertNull(nextState.user)
            assertTrue(nextState.pictures.isEmpty())
            assertNull(nextState.error)

            stateMachine.onEvent(TestEvent.LoadUser)

            nextState = expectItem()
            assertFalse(nextState.isUserLoading)
            assertTrue(nextState.arePicturesLoading)
            assertEquals("John", nextState.user)
            assertTrue(nextState.pictures.isEmpty())
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shouldTransitionAfterTestEventLoadPictures() = runBlockingTest {
        whenever(itemRepository.loadPictures(0, 20))
            .thenReturn(listOf("", "", ""))

        stateMachine.state.test {
            var state = expectItem()
            assertEquals(TestState(), state)

            stateMachine.onEvent(TestEvent.ShowLoading)

            state = expectItem()
            assertTrue(state.isUserLoading)
            assertTrue(state.arePicturesLoading)
            assertNull(state.user)
            assertTrue(state.pictures.isEmpty())
            assertNull(state.error)

            stateMachine.onEvent(TestEvent.LoadPictures(0, 20))

            state = expectItem()
            assertTrue(state.isUserLoading)
            assertFalse(state.arePicturesLoading)
            assertNull(state.user)
            assertTrue(state.pictures.isNotEmpty())
            assertNull(state.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun errorShouldNotBeNullAfterLoadingFails() = runBlockingTest {
        val error = Exception("error")
        whenever(itemRepository.loadPictures(0, 20))
            .thenThrow(error)

        stateMachine.state.test {
            var state = expectItem()
            assertEquals(TestState(), state)

            stateMachine.onEvent(TestEvent.ShowLoading)

            state = expectItem()
            assertTrue(state.isUserLoading)
            assertTrue(state.arePicturesLoading)
            assertNull(state.user)
            assertTrue(state.pictures.isEmpty())
            assertNull(state.error)

            stateMachine.onEvent(TestEvent.LoadPictures(0, 0))

            state = expectItem()
            assertTrue(state.isUserLoading)
            assertFalse(state.arePicturesLoading)
            assertNull(state.user)
            assertTrue(state.pictures.isEmpty())
            assertEquals(TestError.NetworkError, state.error)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

private interface FakeItemRepository {
    @Throws(Exception::class)
    suspend fun loadPictures(offset: Int, limit: Int): List<String>
}

