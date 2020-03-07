package contributors

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import tasks.loadContributorsReactive
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

enum class LoadingStatus { COMPLETED, CANCELED, IN_PROGRESS, WAITING }

class ContributorsViewModel : CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    val contributions: BroadcastChannel<List<User>> = ConflatedBroadcastChannel()

    val loadingStatus: BroadcastChannel<LoadingStatus> = ConflatedBroadcastChannel()

    @ExperimentalTime
    val timeSpent: BroadcastChannel<Duration> = ConflatedBroadcastChannel()

    val isCancelable: BroadcastChannel<Boolean> = ConflatedBroadcastChannel()

    @ExperimentalTime
    fun onStartLoading(req: RequestData) {
        clear()
        val startTime = TimeSource.Monotonic.markNow()
        launch {
            val service = createGithubService(req.username, req.password)
            loadingStatus.send(LoadingStatus.IN_PROGRESS)
            isCancelable.send(true)
            try {
                loadContributorsReactive(service, req)
                    .collect { contributions.send(it); timeSpent.send(startTime.elapsedNow()) }
                loadingStatus.send(LoadingStatus.COMPLETED)
            } catch (e: CancellationException) {
                loadingStatus.send(LoadingStatus.CANCELED)
            }
            isCancelable.send(false)
        }
    }

    fun onCancel() {
        job.cancelChildren()
    }

    private fun clear() {
        launch {
            contributions.send(emptyList())
            loadingStatus.send(LoadingStatus.WAITING)
        }
    }


}