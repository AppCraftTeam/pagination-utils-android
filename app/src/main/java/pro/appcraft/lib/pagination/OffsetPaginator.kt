package pro.appcraft.lib.pagination

import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KSuspendFunction2

private const val ZERO_OFFSET = 0

class OffsetPaginator<T: Any>(
    coroutineScope: CoroutineScope,
    private val requestFactory: KSuspendFunction2<Int, Int, List<T>>,
    viewController: ViewController<T>,
    limit: Int = DEFAULT_LIMIT
) : BasePaginator<T>(coroutineScope, viewController, limit) {
    @Throws(Exception::class)
    override suspend fun loadRequest(currentData: List<T>?, limit: Int): List<T> {
        val offset = currentData?.size ?: ZERO_OFFSET

        return requestFactory(offset, limit)
    }
}
