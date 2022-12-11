package ru.radiationx.anilibria.common

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.radiationx.anilibria.screen.LifecycleViewModel
import ru.radiationx.shared.ktx.coRunCatching
import timber.log.Timber

abstract class BaseCardsViewModel : LifecycleViewModel() {

    val cardsData = MutableLiveData<List<Any>>()
    val rowTitle = MutableLiveData<String>()

    protected open val firstPage = 1
    protected open val perPage = 20
    protected open val loadOnCreate = true
    protected open val progressOnRefresh = true
    open val defaultTitle = "Cards"

    protected open val loadMoreCard = LinkCard("Загрузить еще")
    protected open val loadingCard = LoadingCard("Загрузка данных")

    protected val currentCards = mutableListOf<LibriaCard>()
    protected var currentPage = -1
        private set

    private var requestJob: Job? = null

    override fun onColdCreate() {
        super.onColdCreate()
        rowTitle.value = defaultTitle
    }

    override fun onCreate() {
        super.onCreate()
        if (loadOnCreate) {
            onRefreshClick()
        }
    }

    open fun onLinkCardClick() {
        currentPage++
        loadPage(currentPage)
    }

    open fun onRefreshClick() {
        currentPage = firstPage
        loadPage()
    }

    open fun onLoadingCardClick() {
        loadPage()
    }

    open fun onLibriaCardClick(card: LibriaCard) {}

    protected abstract suspend fun getLoader(requestPage: Int): List<LibriaCard>

    protected open fun hasMoreCards(
        newCards: List<LibriaCard>,
        allCards: List<LibriaCard>
    ): Boolean =
        newCards.size >= 10 && newCards.isNotEmpty()

    protected open fun getErrorCard(error: Throwable) = LoadingCard(
        "Повторить загрузку",
        "Произошла ошибка ${error.message}",
        isError = true
    )

    private fun loadPage(requestPage: Int = currentPage) {
        if (requestPage != firstPage || progressOnRefresh) {
            cardsData.value = currentCards + loadingCard
        }

        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            coRunCatching {
                getLoader(requestPage)
            }.onSuccess { newCards ->
                if (currentPage <= 1) {
                    currentCards.clear()
                }
                currentCards.addAll(newCards)

                if (hasMoreCards(newCards, currentCards)) {
                    cardsData.value = currentCards + loadMoreCard
                } else {
                    cardsData.value = currentCards
                }
            }.onFailure {
                Timber.e(it)
                cardsData.value = currentCards + getErrorCard(it)
            }
        }
    }

}