package pro.appcraft.lib.pagination

import io.reactivex.rxjava3.core.Single

class OffsetPaginator<T: Any> (
    private val requestFactory : (Int, Int) -> Single<List<T>>,
    viewController: ViewController<T>,
    limit: Int = DEFAULT_LIMIT
) : BasePaginator<T>(viewController, limit) {
    @Throws(Exception::class)
    override fun loadRequest(currentData: List<T>?, limit: Int): Single<List<T>> {
        val offset = currentData?.size ?: ZERO_OFFSET

        return requestFactory(offset, limit)
    }

    companion object {
        private const val ZERO_OFFSET = 0
    }
}
