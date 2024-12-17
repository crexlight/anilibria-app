package ru.radiationx.shared_app.controllers.loaderpage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.radiationx.shared.ktx.coRunCatching
import timber.log.Timber

class PageLoader<T>(
    private val coroutineScope: CoroutineScope,
    private val firstPage: Int = 1,
    private val dataSource: suspend (PageLoaderParams<T>) -> PageLoaderAction.Data<T>
) {

    private val _currentPage = MutableStateFlow(firstPage)

    private var loadingJob: Job? = null

    private val _state = MutableStateFlow(PageLoaderState.empty<T>())

    fun observePage(): StateFlow<Int> {
        return _currentPage
    }

    fun observeState(): StateFlow<PageLoaderState<T>> {
        return _state
    }

    fun reset() {
        release()
        _currentPage.value = firstPage
        _state.value = PageLoaderState.empty()
    }

    fun refresh() {
        loadPage(firstPage)
    }

    fun loadMore() {
        if (_state.value.hasMoreData) {
            loadPage(_currentPage.value + 1)
        }
    }

    fun release() {
        loadingJob?.cancel()
    }

    fun getData(): T? {
        return _state.value.data
    }

    fun modifyData(data: T?, hasMoreData: Boolean? = null) {
        val action = PageLoaderAction.ModifyData(data, hasMoreData)
        updateStateByAction(action, createPageLoadParams(_currentPage.value))
    }

    fun modifyData(hasMoreData: Boolean? = null, block: (T) -> T) {
        val newData = _state.value.data?.let(block)
        modifyData(newData, hasMoreData)
    }

    private fun loadPage(page: Int) {
        if (loadingJob?.isActive == true) {
            return
        }

        val params = createPageLoadParams(page)

        val startLoadingAction: PageLoaderAction<T>? = when {
            params.isEmptyLoading -> PageLoaderAction.EmptyLoading()
            params.isRefreshLoading -> PageLoaderAction.Refresh()
            params.isMoreLoading -> PageLoaderAction.MoreLoading()
            else -> null
        }
        if (startLoadingAction != null) {
            updateStateByAction(startLoadingAction, params)
        }

        loadingJob = coroutineScope.launch {
            coRunCatching {
                dataSource.invoke(params)
            }.onSuccess { dataAction ->
                _currentPage.value = page
                updateStateByAction(dataAction, params)
            }.onFailure { error ->
                Timber.e("page=$page", error)
                updateStateByAction(PageLoaderAction.Error(error), params)
            }
        }
    }

    private fun createPageLoadParams(page: Int): PageLoaderParams<T> {
        val isFirstPage = page == firstPage
        val isEmptyData = _state.value.data == null
        return PageLoaderParams(
            page = page,
            isFirstPage = isFirstPage,
            isDataEmpty = isEmptyData,
            isEmptyLoading = isFirstPage && isEmptyData,
            isRefreshLoading = isFirstPage && !isEmptyData,
            isMoreLoading = !isFirstPage,
            currentData = _state.value.data
        )
    }

    private fun updateStateByAction(action: PageLoaderAction<T>, params: PageLoaderParams<T>) {
        _state.update {
            it.applyAction(action, params)
        }
    }

}