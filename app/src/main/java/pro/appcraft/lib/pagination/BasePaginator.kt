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
    internal val currentData: MutableList<T> = ArrayList()

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
        fun updateData() {}
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
                currentState = if (data.size < limit || limit == SINGLE_PAGE_LIMIT)
                    ALL_DATA()
                else
                    DATA()
            }
        }

        override fun fail(error: Throwable) {
            currentState = EMPTY_ERROR(error)
        }

        override fun release() {
            currentState = RELEASED()
        }

        override fun updateData() {
            if (currentData.isNotEmpty()) {
                currentState = PAGE_PROGRESS()
            }
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

        override fun updateData() {
            if (currentData.isNotEmpty()) {
                currentState = DATA()
            }
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

        override fun updateData() {
            if (currentData.isNotEmpty()) {
                currentState = DATA()
            }
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

        override fun updateData() {
            if (currentData.isEmpty())
                currentState = EMPTY_DATA()
            else
                viewController.showData(true, currentData)
        }
    }

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
                currentState = if (data.size < limit || limit == SINGLE_PAGE_LIMIT)
                    ALL_DATA()
                else
                    DATA()
            }
        }

        override fun fail(error: Throwable) {
            currentState = DATA()
            viewController.showErrorMessage(error)
        }

        override fun release() {
            currentState = RELEASED()
        }

        override fun updateData() {
            if (currentData.isEmpty())
                currentState = EMPTY_PROGRESS()
            else
                viewController.showData(true, currentData)
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
            currentState = if (data.isEmpty() || data.size < limit || limit == SINGLE_PAGE_LIMIT)
                ALL_DATA()
            else
                DATA()
        }

        @Throws(Exception::class)
        override fun refresh() {
            currentState = REFRESH()
            load(null)
        }

        override fun fail(error: Throwable) {
            currentState = if (currentData.isEmpty()) EMPTY_DATA() else DATA()
            viewController.showErrorMessage(error)
        }

        override fun release() {
            currentState = RELEASED()
        }

        override fun updateData() {
            if (currentData.isEmpty())
                currentState = EMPTY_PROGRESS()
            else
                viewController.showData(true, currentData)
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

        override fun updateData() {
            if (currentData.isEmpty())
                currentState = EMPTY_DATA()
            else
                viewController.showData(true, currentData)
        }
    }

    private inner class RELEASED internal constructor() : State<T> {
        init {
            currentData.clear()
            if (disposable != null && !disposable!!.isDisposed)
                disposable!!.dispose()
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

    fun invalidate() {
        currentState.updateData()
    }

    fun add(sample: T, position: Int = -1, predicate: (T) -> Boolean = { false }): Int {
        val currentIndex = currentData.indexOfFirst(predicate)
        if (currentIndex >= 0) return -1

        val index = when {
            position < 0 || currentData.lastIndex < 0 -> 0
            position > currentData.lastIndex -> currentData.lastIndex
            else -> position
        }
        currentData.add(index, sample)
        currentState.updateData()

        return index
    }

    fun removeFirst(predicate: (T) -> Boolean): Int {
        val current = currentData.firstOrNull(predicate) ?: return -1
        val currentIndex = currentData.indexOf(current)

        currentData.remove(current)
        currentState.updateData()

        return currentIndex
    }

    fun removeAll(predicate: (T) -> Boolean) {
        currentData.removeAll(currentData.filter(predicate))
        currentState.updateData()
    }

    companion object {
        internal const val DEFAULT_LIMIT = 10
        private const val SINGLE_PAGE_LIMIT = 0
    }
}
