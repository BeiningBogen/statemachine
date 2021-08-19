package no.beiningbogen.statemachine

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

data class Customer(
    val id: Int,
    val name: String,
)

data class CustomerScreenState(
    val isLoading: Boolean = false,
    val customers: List<Customer> = emptyList(),
    val error: TestError? = null,
)

sealed class TestError {
    abstract val message: String

    object NetworkError : TestError() {
        override val message = "something went wrong, try again later"
    }
}

sealed class CustomerScreenEvents {
    object ShowLoading : CustomerScreenEvents()
    object HideLoading : CustomerScreenEvents()
    object LoadCustomers : CustomerScreenEvents()
    data class LoadCustomerWithName(val name: String) : CustomerScreenEvents()
}

@ExperimentalTime
@ExperimentalCoroutinesApi
class StateMachineTest {

    private lateinit var builder: StateMachine<CustomerScreenState, CustomerScreenEvents>.() -> Unit
    private lateinit var stateMachine: StateMachine<CustomerScreenState, CustomerScreenEvents>

    private val dispatcher = TestCoroutineDispatcher()
    private val customers = listOf(
        Customer(id = 0, name = "John"),
        Customer(id = 1, name = "Emma"),
    )

    private val loadCustomerTransition = object : Transition<CustomerScreenState, CustomerScreenEvents.LoadCustomers> {
        override val isExecutable: (CustomerScreenState) -> Boolean = { it.isLoading }
        override val execute: suspend (CustomerScreenEvents.LoadCustomers, MutableStateFlow<CustomerScreenState>) -> Unit =
            { event, state ->
                // do some IO operation to load customers
                state.value = state.value.copy(customers = customers)
            }
    }

    private val loadCustomerWithNameTransition =
        object : Transition<CustomerScreenState, CustomerScreenEvents.LoadCustomerWithName> {
            override val isExecutable: (CustomerScreenState) -> Boolean = { it.isLoading }
            override val execute: suspend (CustomerScreenEvents.LoadCustomerWithName, MutableStateFlow<CustomerScreenState>) -> Unit =
                { event, state ->
                    // do some IO operation to load customers
                    state.value = state.value.copy(customers = loadWithFilter(event.name))
                }

            private fun loadWithFilter(name: String): List<Customer> {
                // use the name value from the event to filter the search or whatever.
                return listOf(Customer(id = 0, name = "John"))
            }
        }

    @Before
    fun setUp() {
        /**
         * Create and initialize a state machine with a builder lambda.
         */
        builder = {

            /**
             * Register a predefined transition.
             */
            register(loadCustomerTransition)
            register(loadCustomerWithNameTransition)

            /**
             * Register an anonymous transition here
             */
            register {
                transition<CustomerScreenState, CustomerScreenEvents.ShowLoading>(
                    predicate = { !it.isLoading },
                    execution = { event, state ->
                        state.value = state.value.copy(isLoading = true)
                    }
                )
            }

            register {
                transition<CustomerScreenState, CustomerScreenEvents.HideLoading>(
                    predicate = { it.isLoading },
                    execution = { event, state ->
                        state.value = state.value.copy(isLoading = false)
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
    fun shouldTransitionToLoadingState() = dispatcher.runBlockingTest {
        val initialState = CustomerScreenState()
        stateMachine = createStateMachine(initialState, dispatcher, builder)

        stateMachine.state.test {
            assertEquals(initialState, expectItem())
            stateMachine.onEvent(CustomerScreenEvents.ShowLoading)

            val nextState = expectItem()
            assertTrue(nextState.isLoading)
            assertTrue(nextState.customers.isEmpty())
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shouldTransitionToCustomerLoadedState() = dispatcher.runBlockingTest {
        val initialState = CustomerScreenState(isLoading = true)
        stateMachine = createStateMachine(initialState, dispatcher, builder)

        stateMachine.state.test {
            assertEquals(initialState, expectItem())
            stateMachine.onEvent(CustomerScreenEvents.LoadCustomers)

            val nextState = expectItem()
            assertTrue(nextState.isLoading)
            assertEquals(customers, nextState.customers)
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shouldTransitionToFilteredCustomerLoadedState() = dispatcher.runBlockingTest {
        val initialState = CustomerScreenState(isLoading = true)
        stateMachine = createStateMachine(initialState, dispatcher, builder)

        stateMachine.state.test {
            assertEquals(initialState, expectItem())
            stateMachine.onEvent(CustomerScreenEvents.LoadCustomerWithName("John"))

            val nextState = expectItem()
            assertTrue(nextState.isLoading)
            assertEquals(listOf(Customer(id = 0, name = "John")), nextState.customers)
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shouldTransitionToHideLoadingState() = dispatcher.runBlockingTest {
        val initialState = CustomerScreenState(isLoading = true, customers = customers)
        stateMachine = createStateMachine(initialState, dispatcher, builder)

        stateMachine.state.test {
            assertEquals(initialState, expectItem())
            stateMachine.onEvent(CustomerScreenEvents.HideLoading)

            val nextState = expectItem()
            assertFalse(nextState.isLoading)
            assertEquals(customers, nextState.customers)
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
