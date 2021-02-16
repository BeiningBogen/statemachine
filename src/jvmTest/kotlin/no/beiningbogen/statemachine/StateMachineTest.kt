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
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class StateMachineTest {

    private lateinit var stateMachine: StateMachine<TestStates, AppEvent>
    private lateinit var items: List<Item>
    private lateinit var itemRepository: FakeItemRepository

    private val dispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        items = mock()
        itemRepository = mock()

        stateMachine = StateMachine.create(TestStates.Initial, dispatcher) {
            /**
             * Define on which states the transitions register inside the lambda should applied to.
             */
            states(TestStates.Initial::class, TestStates.Loaded::class) {

                /**
                 * Register a lambda triggered by a specific event executing some suspending
                 * code and returning a new state.
                 */
                on<AppEvent.ShowLoading> { event, sendChannel ->
                    sendChannel.send(TestStates.Loading)
                }
            }

            /**
             * In order to use states requiring parameters, the previous transition could have been register with
             * states(AppState.Initial::class, AppState.AnotherState::class)
             */

            states(TestStates.Loading::class, TestStates.Error::class) {
                on<AppEvent.LoadData> { event, sendChannel ->
                    sendChannel.send(TestStates.Loaded(items))
                }

                on<AppEvent.SearchItemByName> { event, sendChannel ->
                    try {
                        val data = itemRepository.search(event.name, event.page)
                        sendChannel.send(TestStates.Loaded(data))
                    } catch (e: Exception) {
                        sendChannel.send(TestStates.Error(e.localizedMessage))
                    }
                }
            }
        }
    }

    @After
    fun cleanUp() {
        stateMachine.destroy()
    }

    @Test
    fun `initial state should be State_Initial`() = dispatcher.runBlockingTest {
        assertEquals(TestStates.Initial, stateMachine.state.first())
    }

    @Test
    fun `should transition to State_Loading`() = dispatcher.runBlockingTest {
        stateMachine.state.test {
            var nextState = expectItem()
            assertEquals(TestStates.Initial, nextState)

            stateMachine.onEvent(AppEvent.ShowLoading)

            nextState = expectItem()
            assertEquals(TestStates.Loading, nextState)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should transition to State_Loaded after loading`() = dispatcher.runBlockingTest {
        stateMachine.state.test {
            var nextState = expectItem()
            assertEquals(TestStates.Initial, nextState)

            stateMachine.onEvent(AppEvent.ShowLoading)

            nextState = expectItem()
            assertEquals(TestStates.Loading, nextState)

            stateMachine.onEvent(AppEvent.LoadData)

            nextState = expectItem()
            assertTrue(nextState is TestStates.Loaded<*>)
            assertEquals(items, nextState.data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should transition to State_Loaded after search`() = runBlockingTest {
        whenever(itemRepository.search("name", 0))
            .thenReturn(items)

        stateMachine.state.test {
            val state = expectItem()
            assertEquals(TestStates.Initial, state)

            stateMachine.onEvent(AppEvent.ShowLoading)

            val state2 = expectItem()
            assertEquals(TestStates.Loading, state2)

            stateMachine.onEvent(AppEvent.SearchItemByName("name", 0))

            val state3 = expectItem()
            assertTrue(state3 is TestStates.Loaded<*>)
            assertEquals(items, state3.data)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `should transition to Error and State_Loaded after retry`() = runBlockingTest {
        val error = Exception("error")
        whenever(itemRepository.search("name", 0))
            .thenThrow(error)
            .thenReturn(items)

        stateMachine.state.test {
            val state = expectItem()
            assertEquals(TestStates.Initial, state)

            stateMachine.onEvent(AppEvent.ShowLoading)

            val state2 = expectItem()
            assertEquals(TestStates.Loading, state2)

            stateMachine.onEvent(AppEvent.SearchItemByName("name", 0))

            val state3 = expectItem()
            assertTrue(state3 is TestStates.Error)

            stateMachine.onEvent(AppEvent.SearchItemByName("name", 0))

            val state4 = expectItem()
            assertTrue(state4 is TestStates.Loaded<*>)
            assertEquals(items, state4.data)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

private interface FakeItemRepository {
    @Throws(Exception::class)
    suspend fun search(name: String, page: Int): List<Item>
}

