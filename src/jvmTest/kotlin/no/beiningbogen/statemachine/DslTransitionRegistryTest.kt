package no.beiningbogen.statemachine

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DslTransitionRegistryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var registry: DslTransitionRegistry<AppState, AppEvent>
    private lateinit var items: List<Item>

    @Before
    fun setUp() {
        registry = DslTransitionRegistry()
        items = mock()
    }

    @Test
    fun `add transitions to the registry and fetch them`() = coroutineTestRule.runBlockingTest {
        val initialState = AppState.Initial
        val showLoadingEvent = AppEvent.ShowLoading
        val showLoadingTransition = DslTransition<AppState.Loading, AppEvent.ShowLoading> { AppState.Loading }

        registry.registerTransition(initialState::class, showLoadingEvent::class, showLoadingTransition)

        val loadingState = AppState.Loading
        val loadDataEvent = AppEvent.LoadData
        val dataLoadedTransition = DslTransition<AppState.Loaded<List<Item>>, AppEvent.LoadData> { AppState.Loaded(items) }

        registry.registerTransition(loadingState::class, loadDataEvent::class, dataLoadedTransition)

        val showLoadingFoundTransition =
            registry.findTransition<AppEvent.ShowLoading>(initialState::class, showLoadingEvent::class)
        assertNotNull(showLoadingFoundTransition)

        val showLoadingResultState = showLoadingFoundTransition.block(showLoadingEvent)

        assertEquals(AppState.Loading, showLoadingResultState)

        val loadingFoundTransition =
            registry.findTransition<AppEvent.LoadData>(loadingState::class, loadDataEvent::class)
        assertNotNull(loadingFoundTransition)

        val loadingResultState = loadingFoundTransition.block(loadDataEvent)

        assertTrue(loadingResultState is AppState.Loaded<*>)
        assertEquals(items, loadingResultState.data)
    }
}
