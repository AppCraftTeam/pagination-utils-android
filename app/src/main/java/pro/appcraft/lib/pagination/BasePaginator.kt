package pro.appcraft.lib.pagination

import io.reactivex.Single
import io.reactivex.disposables.Disposable
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

abstract class BasePaginator<T : Any>(
    internal val viewController: ViewController<T>,
    var limit: Int = DEFAULT_LIMIT
) {
    val currentData: MutableList<T> = ArrayList()

    internal var currentState: State<T> = IDLE()
    private var disposable: Disposable? = null

    fun restart() {
        try {
            currentState.restart()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }

    }

    fun refresh() {
        try {
            currentState.refresh()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }

    }

    fun loadNext() {
        try {
            currentState.loadNext()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }

    }

    fun release() {
        try {
            currentState.release()
        } catch (e: Exception) {
            logger.error(e.message, e)
        }

    }

    fun get(index: Int): T {
        return currentData[index]
    }

    fun size(): Int {
        return currentData.size
    }

    interface ViewController<T> {
        fun showEmptyProgress(show: Boolean) {}

        fun showEmptyError(show: Boolean, error: Throwable?) {}

        fun showEmptyView(show: Boolean) {}

        fun showData(show: Boolean, data: List<T>) {}

        fun showErrorMessage(error: Throwable) {}

        fun showRefreshProgress(show: Boolean) {}

        fun showPageProgress(show: Boolean) {}
    }

    interface State<T> {
        @Throws(Exception::class)
        fun restart() {
        }

        @Throws(Exception::class)
        fun refresh() {
        }

        @Throws(Exception::class)
        fun loadNext() {
        }

        fun release() {}

        fun newData(data: List<T>) {}

        fun fail(error: Throwable) {}
    }

    private inner class IDLE internal constructor() : State<T> {
        init {
            viewController.showRefreshProgress(false)
            viewController.showPageProgress(false)
            viewController.showEmptyView(false)
            viewController.showEmptyProgress(false)
            viewController.showEmptyError(false, null)
            viewController.showData(false, currentData)
        }

        @Throws(Exception::class)
        override fun restart() {
            currentData.clear()
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        @Throws(Exception::class)
        override fun refresh() {
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    private inner class EMPTY_PROGRESS internal constructor() : State<T> {
        init {
            viewController.showRefreshProgress(false)
            viewController.showPageProgress(false)
            viewController.showEmptyView(false)
            viewController.showEmptyError(false, null)
            viewController.showData(false, currentData)

            viewController.showEmptyProgress(true)
        }

        @Throws(Exception::class)
        override fun restart() {
            load(null)
        }

        override fun newData(data: List<T>) {
            if (data.isEmpty()) {
                currentData.clear()
                currentState = EMPTY_DATA()
            } else {
                currentData.clear()
                currentData.addAll(data)
                currentState = if (data.size < limit || limit == SINGLE_PAGE_LIMIT) {
                    ALL_DATA()
                } else {
                    DATA()
                }
            }
        }

        override fun fail(error: Throwable) {
            currentState = EMPTY_ERROR(error)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    private inner class EMPTY_ERROR internal constructor(error: Throwable) : State<T> {
        init {
            viewController.showRefreshProgress(false)
            viewController.showPageProgress(false)
            viewController.showData(false, currentData)
            viewController.showEmptyProgress(false)
            viewController.showEmptyView(false)

            viewController.showEmptyError(true, error)
        }

        @Throws(Exception::class)
        override fun restart() {
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        @Throws(Exception::class)
        override fun refresh() {
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    internal inner class EMPTY_DATA : State<T> {
        init {
            viewController.showRefreshProgress(false)
            viewController.showPageProgress(false)
            viewController.showEmptyError(false, null)
            viewController.showData(false, currentData)
            viewController.showEmptyProgress(false)

            viewController.showEmptyView(true)
        }

        @Throws(Exception::class)
        override fun restart() {
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        @Throws(Exception::class)
        override fun refresh() {
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    private inner class DATA internal constructor() : State<T> {
        init {
            viewController.showRefreshProgress(false)
            viewController.showPageProgress(false)
            viewController.showEmptyError(false, null)
            viewController.showEmptyProgress(false)
            viewController.showEmptyView(false)

            viewController.showData(true, currentData)
        }

        @Throws(Exception::class)
        override fun restart() {
            currentData.clear()
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        @Throws(Exception::class)
        override fun refresh() {
            currentState = REFRESH()
            load(null)
        }

        @Throws(Exception::class)
        override fun loadNext() {
            currentState = PAGE_PROGRESS()
            load(currentData)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    @Throws(Exception::class)
    private fun load(currentData: List<T>?) {
        disposable?.dispose()

        disposable = loadRequest(currentData, limit)
            .subscribe({ result -> currentState.newData(result) }, { error -> currentState.fail(error) })
    }

    @Throws(Exception::class)
    protected abstract fun loadRequest(currentData: List<T>?, limit: Int): Single<List<T>>

    private inner class REFRESH internal constructor() : State<T> {
        init {
            viewController.showPageProgress(false)
            viewController.showEmptyError(false, null)
            viewController.showEmptyProgress(false)
            viewController.showEmptyView(false)

            viewController.showData(true, currentData)
            viewController.showRefreshProgress(true)
        }

        @Throws(Exception::class)
        override fun restart() {
            currentData.clear()
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        override fun newData(data: List<T>) {
            if (data.isEmpty()) {
                currentData.clear()
                currentState = EMPTY_DATA()
            } else {
                currentData.clear()
                currentData.addAll(data)
                currentState = if (data.size < limit || limit == SINGLE_PAGE_LIMIT) {
                    ALL_DATA()
                } else {
                    DATA()
                }
            }
        }

        override fun fail(error: Throwable) {
            currentData.clear()
            currentState = EMPTY_ERROR(error)
//            viewController.showErrorMessage(error)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    private inner class PAGE_PROGRESS internal constructor() : State<T> {
        init {
            viewController.showEmptyError(false, null)
            viewController.showEmptyProgress(false)
            viewController.showEmptyView(false)
            viewController.showRefreshProgress(false)

            viewController.showData(true, currentData)
            viewController.showPageProgress(true)
        }

        @Throws(Exception::class)
        override fun restart() {
            currentData.clear()
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        override fun newData(data: List<T>) {
            currentData.addAll(data)
            currentState = if (data.isEmpty() || data.size < limit || limit == SINGLE_PAGE_LIMIT) {
                ALL_DATA()
            } else {
                DATA()
            }
        }

        @Throws(Exception::class)
        override fun refresh() {
            currentState = REFRESH()
            load(null)
        }

        override fun fail(error: Throwable) {
            currentState = DATA()
            viewController.showErrorMessage(error)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    private inner class ALL_DATA internal constructor() : State<T> {
        init {
            viewController.showRefreshProgress(false)
            viewController.showPageProgress(false)
            viewController.showEmptyError(false, null)
            viewController.showEmptyProgress(false)
            viewController.showEmptyView(false)

            viewController.showData(true, currentData)
        }

        @Throws(Exception::class)
        override fun restart() {
            currentData.clear()
            currentState = EMPTY_PROGRESS()
            load(null)
        }

        @Throws(Exception::class)
        override fun refresh() {
            currentState = REFRESH()
            load(null)
        }

        override fun release() {
            currentState = RELEASED()
        }
    }

    private inner class RELEASED internal constructor() : State<T> {
        init {
            if (disposable != null && !disposable!!.isDisposed) {
                disposable!!.dispose()
            }
        }
    }

    fun update(sample: T, predicate: (T) -> Boolean) {
        val currentIndex = currentData.indexOfFirst(predicate)
        if (currentIndex < 0) {
            return
        }

        currentData[currentIndex] = sample

        viewController.showData(true, currentData)
    }

    fun remove(predicate: (T) -> Boolean) {
        val current = currentData.firstOrNull(predicate) ?: return

        currentData.remove(current)
        if (currentData.isEmpty()) {
            currentState = EMPTY_DATA()
        } else {
            viewController.showData(true, currentData)
        }
    }

    companion object {
        internal const val DEFAULT_LIMIT = 20
        private const val SINGLE_PAGE_LIMIT = 0
    }
}
