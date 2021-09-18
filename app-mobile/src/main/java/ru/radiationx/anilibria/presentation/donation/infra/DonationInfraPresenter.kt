package ru.radiationx.anilibria.presentation.donation.infra

import ru.radiationx.anilibria.presentation.common.BasePresenter
import ru.radiationx.anilibria.ui.common.ErrorHandler
import ru.radiationx.anilibria.utils.Utils
import ru.radiationx.data.analytics.features.DonationInfraAnalytics
import ru.radiationx.data.entity.app.donation.other.DonationInfraInfo
import ru.radiationx.data.repository.DonationRepository
import ru.terrakok.cicerone.Router
import toothpick.InjectConstructor

@InjectConstructor
class DonationInfraPresenter(
    router: Router,
    private val donationRepository: DonationRepository,
    private val errorHandler: ErrorHandler,
    private val analytics: DonationInfraAnalytics
) : BasePresenter<DonationInfraView>(router) {

    private var currentData: DonationInfraInfo? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        donationRepository
            .observerDonationDetail()
            .subscribe({
                val infraInfo = it.otherSupport?.btInfra?.info
                if (infraInfo != null) {
                    currentData = infraInfo
                    viewState.showData(infraInfo)
                }
            }, {
                errorHandler.handle(it)
            })
            .addToDisposable()
    }

    fun onTelegramClick() {
        analytics.telegramClick()
        currentData?.btTelegram?.link?.let { Utils.externalLink(it) }
    }

    fun onLinkClick(url: String) {
        analytics.linkClick(url)
        Utils.externalLink(url)
    }

}