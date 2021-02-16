package no.beiningbogen.statemachine

import app.cash.turbine.test
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalCoroutinesApi
class TransitionRegistryTest {

    private lateinit var registry: TransitionRegistry<TestStates, AppEvent>
    private lateinit var items: List<Item>

    private val dispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        registry = TransitionRegistry()
        items = mock()
    }

    @Test
    fun `add transitions to the registry and fetch them`() = dispatcher.runBlockingTest {
        val showLoadingTransition = TransitionWrapper<TestStates, AppEvent.ShowLoading> { event, sendChannel ->
            sendChannel.offer(TestStates.Loading)
        }

        val dataLoadedTransition = TransitionWrapper<TestStates, AppEvent.LoadData> { event, sendChannel ->
            sendChannel.offer(TestStates.Loaded(items))
        }

        registry.registerTransition(TestStates.Initial::class, AppEvent.ShowLoading::class, showLoadingTransition)
        registry.registerTransition(TestStates.Loading::class, AppEvent.LoadData::class, dataLoadedTransition)

        val channel = Channel<TestStates>()
        channel.receiveAsFlow().test {
            val showLoadingFoundTransition = registry.findTransitionWrapper(TestStates.Initial::class, AppEvent.ShowLoading::class)
            assertNotNull(showLoadingFoundTransition)

            showLoadingFoundTransition.transition(AppEvent.ShowLoading, channel)

            assertEquals(TestStates.Loading, expectItem())

            val loadingFoundTransition = registry.findTransitionWrapper(TestStates.Loading::class, AppEvent.LoadData::class)
            assertNotNull(loadingFoundTransition)

            loadingFoundTransition.transition(AppEvent.LoadData, channel)

            val loadedState = expectItem()
            assertTrue(loadedState is TestStates.Loaded<*>)
            assertEquals(items, loadedState.data)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
