package no.beiningbogen.statemachine

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.*
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
    data class CustomerSelected(val id: Int) : CustomerScreenEvents()
    object ShowAboutApp : CustomerScreenEvents()
}

sealed class CustomerScreenSideEffect {
    data class NavigateToCustomerDetails(val id: Int) : CustomerScreenSideEffect()
    object ShowAboutApp : CustomerScreenSideEffect()
}

@ExperimentalTime
@ExperimentalCoroutinesApi
class StateMachineTest {

    private lateinit var builder: StateMachine<CustomerScreenState, CustomerScreenEvents, CustomerScreenSideEffect>.() -> Unit
    private lateinit var stateMachine: StateMachine<CustomerScreenState, CustomerScreenEvents, CustomerScreenSideEffect>

    private val dispatcher = TestCoroutineDispatcher()
    private val customers = listOf(
        Customer(id = 0, name = "John"),
        Customer(id = 1, name = "Emma"),
    )

    private val loadCustomerTransition = object :
        Transition<CustomerScreenState, CustomerScreenEvents.LoadCustomers> {
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

    private val customerSelectedSideEffectTransition = object :
        SideEffectTransition<CustomerScreenState, CustomerScreenEvents.CustomerSelected, CustomerScreenSideEffect> {
        override val isExecutable: (CustomerScreenState) -> Boolean = { !it.isLoading }
        override val execute: suspend (CustomerScreenEvents.CustomerSelected, MutableSharedFlow<CustomerScreenSideEffect>) -> Unit =
            { event, sideEffect ->
                sideEffect.emit(CustomerScreenSideEffect.NavigateToCustomerDetails(event.id))
            }
    }

    @BeforeTest
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
            register(customerSelectedSideEffectTransition)

            /**
             * Register an anonymous transition here
             */
            registerTransition {
                transition<CustomerScreenState, CustomerScreenEvents.ShowLoading>(
                    predicate = { !it.isLoading },
                    execution = { event, state ->
                        state.value = state.value.copy(isLoading = true)
                    }
                )
            }

            registerTransition {
                transition<CustomerScreenState, CustomerScreenEvents.HideLoading>(
                    predicate = { it.isLoading },
                    execution = { event, state ->
                        state.value = state.value.copy(isLoading = false)
                    }
                )
            }

            registerSideEffect {
                sideEffectTransition<CustomerScreenState, CustomerScreenEvents.ShowAboutApp, CustomerScreenSideEffect>(
                    predicate = { !it.isLoading },
                    execution = { event, sideEffect ->
                        sideEffect.emit(CustomerScreenSideEffect.ShowAboutApp)
                    }
                )
            }
        }
    }

    @AfterTest
    fun cleanUp() {
        stateMachine.destroy()
    }

    @Test
    fun shouldTransitionToLoadingState() = dispatcher.runBlockingTest {
        val initialState = CustomerScreenState()
        stateMachine = createStateMachine(initialState, dispatcher, builder)

        stateMachine.state.test {
            assertEquals(initialState, awaitItem())
            stateMachine.onEvent(CustomerScreenEvents.ShowLoading)

            val nextState = awaitItem()
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
            assertEquals(initialState, awaitItem())
            stateMachine.onEvent(CustomerScreenEvents.LoadCustomers)

            val nextState = awaitItem()
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
            assertEquals(initialState, awaitItem())
            stateMachine.onEvent(CustomerScreenEvents.LoadCustomerWithName("John"))

            val nextState = awaitItem()
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
            assertEquals(initialState, awaitItem())
            stateMachine.onEvent(CustomerScreenEvents.HideLoading)

            val nextState = awaitItem()
            assertFalse(nextState.isLoading)
            assertEquals(customers, nextState.customers)
            assertNull(nextState.error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun shouldTriggerNavigateToCustomerDetailsSideEffect() = dispatcher.runBlockingTest {
        val initialState = CustomerScreenState()
        stateMachine = createStateMachine(initialState, dispatcher, builder)

        stateMachine.state.test {
            assertEquals(initialState, awaitItem())
            assertTrue(cancelAndConsumeRemainingEvents().isEmpty())
        }

        stateMachine.sideEffects.test {
            stateMachine.onEvent(CustomerScreenEvents.CustomerSelected(1))

            val nextSideEffect = awaitItem()
            assertTrue {
                nextSideEffect is CustomerScreenSideEffect.NavigateToCustomerDetails &&
                        nextSideEffect.id == 1
            }
        }
    }

    @Test
    fun shouldTriggerShowAboutAppSideEffect() = dispatcher.runBlockingTest {
        val initialState = CustomerScreenState()
        stateMachine = createStateMachine(initialState, dispatcher, builder)

        stateMachine.state.test {
            assertEquals(initialState, awaitItem())
            assertTrue(cancelAndConsumeRemainingEvents().isEmpty())
        }

        stateMachine.sideEffects.test {
            stateMachine.onEvent(CustomerScreenEvents.ShowAboutApp)
            assertTrue(awaitItem() is CustomerScreenSideEffect.ShowAboutApp)
        }
    }
}
