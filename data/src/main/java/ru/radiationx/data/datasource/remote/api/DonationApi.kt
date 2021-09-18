package ru.radiationx.data.datasource.remote.api

import android.util.Log
import com.google.gson.Gson
import io.reactivex.Single
import org.json.JSONArray
import org.json.JSONObject
import ru.radiationx.data.ApiClient
import ru.radiationx.data.MainClient
import ru.radiationx.data.datasource.remote.ApiResponse
import ru.radiationx.data.datasource.remote.IClient
import ru.radiationx.data.datasource.remote.address.ApiConfig
import ru.radiationx.data.entity.app.donation.DonationDetail
import ru.radiationx.data.entity.app.donation.donate.DonationYooMoneyInfo
import toothpick.InjectConstructor

@InjectConstructor
class DonationApi(
    @ApiClient private val client: IClient,
    @MainClient private val mainClient: IClient,
    private val apiConfig: ApiConfig,
    private val gson: Gson
) {

    fun getDonationDetail(): Single<DonationDetail> {
        val args: Map<String, String> = mapOf(
            "query" to "donation_details"
        )
        return client.post(apiConfig.apiUrl, args)
            .compose(ApiResponse.fetchResult<JSONObject>())
            .map { gson.fromJson(it.toString(), DonationDetail::class.java) }
    }

    // Doc https://yoomoney.ru/docs/payment-buttons/using-api/forms
    fun createYooMoneyPayLink(
        amount: Int,
        type: String,
        form: DonationYooMoneyInfo.YooMoneyForm
    ): Single<String> {
        val yooMoneyType = when (type) {
            DonationYooMoneyInfo.TYPE_ID_ACCOUNT -> "PC"
            DonationYooMoneyInfo.TYPE_ID_CARD -> "AC"
            DonationYooMoneyInfo.TYPE_ID_MOBILE -> "MC"
            else -> null
        }
        val params = mapOf(
            "receiver" to form.receiver,
            "quickpay-form" to "shop",
            "targets" to form.target,
            "paymentType" to yooMoneyType.orEmpty(),
            "sum" to amount.toString(),
            "formcomment" to form.shortDesc.orEmpty(),
            "short-dest" to form.shortDesc.orEmpty(),
            "label" to form.label.orEmpty()
        )

        return mainClient.postFull("https://yoomoney.ru/quickpay/confirm.xml", params)
            .map { it.redirect }
    }
}