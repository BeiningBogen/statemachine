package no.beiningbogen.statemachine

import app.cash.turbine.test
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import kotlin.test.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class TransitionRegistryTest {

    private lateinit var registry: TransitionRegistry<TestState, TestEvent>
    private lateinit var items: List<Item>
    private lateinit var stateMachine: StateMachine<TestState, TestEvent>

    private val dispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        registry = TransitionRegistry()
        items = mock()
        stateMachine = mock()
    }

    @Test
    fun `add transitions to the registry and fetch them`() = dispatcher.runBlockingTest {
        val showLoadingTransition = TransitionWrapper<TestState, TestEvent.ShowLoading> { transitionUtils ->
            transitionUtils.offer(
                TestState(
                    isUserLoading = true,
                    arePicturesLoading = true,
                )
            )
        }
        registry.registerTransition(TestEvent.ShowLoading::class, showLoadingTransition)

        val loadUserTransition = TransitionWrapper<TestState, TestEvent.LoadUser> { transitionUtils ->
            transitionUtils.send(
                TestState(
                    isUserLoading = false,
                    arePicturesLoading = true,
                    user = "John"
                )
            )
        }
        registry.registerTransition(TestEvent.LoadUser::class, loadUserTransition)

        val channel = Channel<TestState>()
        channel.receiveAsFlow().test {
            val showLoadingFoundTransition = registry.findTransitionWrapper(TestEvent.ShowLoading::class)
            assertNotNull(showLoadingFoundTransition)

            showLoadingFoundTransition.transition(stateMachine.TransitionUtils(channel, TestEvent.ShowLoading))

            var state = expectItem()
            assertTrue(state.isUserLoading)
            assertTrue(state.arePicturesLoading)
            assertNull(state.user)
            assertTrue(state.pictures.isEmpty())
            assertNull(state.error)

            val loadingFoundTransition = registry.findTransitionWrapper(TestEvent.LoadUser::class)
            assertNotNull(loadingFoundTransition)

            loadingFoundTransition.transition(stateMachine.TransitionUtils(channel, TestEvent.LoadUser))

            state = expectItem()
            assertFalse(state.isUserLoading)
            assertTrue(state.arePicturesLoading)
            assertEquals("John", state.user)
            assertTrue(state.pictures.isEmpty())
            assertNull(state.error)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
