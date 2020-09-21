package no.beiningbogen.statemachine

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class StateMachineTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var stateMachine: StateMachine
    private lateinit var items: List<Item>
    private lateinit var itemRepository: FakeItemRepository

    @Before
    fun setUp() {
        items = mock()
        itemRepository = mock()

        stateMachine = StateMachine.create {
            /**
             * Define on which states the transitions register inside the lambda should applied to.
             */
            states(State.Initial, AppState.AnotherState) {

                /**
                 * Register a lambda triggered by a specific event executing some suspending
                 * code and returning a new state.
                 */
                on<AppEvent.ShowLoading> {
                    AppState.Loading
                }
            }

            state<AppState.Loading> {
                on<AppEvent.LoadData> {
                    AppState.Loaded(items)
                }

                on<AppEvent.SearchItemByName> {
                    val data = itemRepository.search(it.name, it.page)
                    when (data) {
                        is Either.Success -> AppState.Loaded(data)
                        is Either.Failure -> State.Error(AppError.NetworkError)
                    }
                }
            }
        }
    }

    @Test
    fun `initial state should be State_Initial`() {
        assertEquals(State.Initial, stateMachine.state)
    }

    @Test
    fun `should transition to State_Loading`() = coroutineTestRule.runBlockingTest {
        val newState = stateMachine.onEvent(AppEvent.ShowLoading)
        assertEquals(AppState.Loading, newState)
        assertEquals(AppState.Loading, stateMachine.state)
    }

    @Test
    fun `should transition to State_Loaded`() = coroutineTestRule.runBlockingTest {
        stateMachine.state = AppState.Loading

        val nextValue = stateMachine.onEvent(AppEvent.LoadData)
        assertEquals(nextValue, stateMachine.state)
        assertTrue(nextValue is AppState.Loaded<*>)
        assertEquals(items, nextValue.data)
    }

    @Test
    fun `should transition to State_Loaded after search`() = coroutineTestRule.runBlockingTest {
        whenever(itemRepository.search("name", 0))
            .thenReturn(Either.Success(items))

        stateMachine.state = AppState.Loading

        val newState = stateMachine.onEvent(AppEvent.SearchItemByName("name", 0))
        assertEquals(newState, stateMachine.state)
        assertTrue(newState is AppState.Loaded<*>)
        assertTrue(newState.data is Either.Success<*>)
        assertEquals(items, newState.data.value)
    }

    @Test
    fun `should transition to Error and State_Loaded after retry`() = coroutineTestRule.runBlockingTest {
        whenever(itemRepository.search("name", 0))
            .thenReturn(Either.Failure(AppError.NetworkError), Either.Success(items))

        stateMachine.state = AppState.Loading

        val errorState = stateMachine.onEvent(AppEvent.SearchItemByName("name", 0))
        assertEquals(errorState, stateMachine.state)
        assertTrue(errorState is State.Error)
        assertEquals(AppError.NetworkError, errorState.error)

        val newState = stateMachine.retry(AppEvent.SearchItemByName("name", 0), 1000)
        assertEquals(newState, stateMachine.state)
        assertTrue(newState is AppState.Loaded<*>)
        assertTrue(newState.data is Either.Success<*>)
        assertEquals(items, newState.data.value)
    }
}

private interface FakeItemRepository {
    suspend fun search(name: String, page: Int): Either<List<Item>, AppError>
}

private sealed class Either<out A, out B> {
    data class Success<A>(val value: A) : Either<A, Nothing>()
    data class Failure<B>(val value: B) : Either<Nothing, B>()
}
