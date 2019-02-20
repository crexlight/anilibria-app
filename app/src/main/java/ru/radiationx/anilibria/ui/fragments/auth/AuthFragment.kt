package ru.radiationx.anilibria.ui.fragments.auth

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import kotlinx.android.synthetic.main.fragment_auth.*
import kotlinx.android.synthetic.main.fragment_main_base.*
import ru.radiationx.anilibria.App
import ru.radiationx.anilibria.R
import ru.radiationx.anilibria.entity.app.auth.SocialAuth
import ru.radiationx.anilibria.extension.addTextChangeListener
import ru.radiationx.anilibria.extension.getColorFromAttr
import ru.radiationx.anilibria.model.data.remote.Api
import ru.radiationx.anilibria.presentation.auth.AuthPresenter
import ru.radiationx.anilibria.presentation.auth.AuthView
import ru.radiationx.anilibria.ui.common.RouterProvider
import ru.radiationx.anilibria.ui.fragments.BaseFragment
import ru.radiationx.anilibria.utils.Utils

/**
 * Created by radiationx on 30.12.17.
 */
class AuthFragment : BaseFragment(), AuthView {

    private val socialAuthAdapter = SocialAuthAdapter {
        presenter.onSocialClick(it)
    }

    @InjectPresenter
    lateinit var presenter: AuthPresenter

    @ProvidePresenter
    fun provideAuthPresenter(): AuthPresenter = AuthPresenter(
            (activity as RouterProvider).getRouter(),
            App.injections.authRepository,
            App.injections.errorHandler
    )

    override fun getLayoutResource(): Int = R.layout.fragment_auth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setStatusBarVisibility(true)
        setStatusBarColor(view.context.getColorFromAttr(R.attr.cardBackground))

        appbarLayout.visibility = View.GONE

        authSocialList.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = socialAuthAdapter
        }

        authSubmit.setOnClickListener { presenter.signIn() }
        authSkip.setOnClickListener { presenter.skip() }
        authRegistration.setOnClickListener { presenter.registrationClick() }

        authLogin.addTextChangeListener { presenter.setLogin(it) }
        authPassword.addTextChangeListener { presenter.setPassword(it) }
        authPassword.addTextChangeListener { presenter.setCode2fa(it) }
    }

    override fun setSignButtonEnabled(isEnabled: Boolean) {
        authSubmit.isEnabled = isEnabled
    }

    override fun showRegistrationDialog() {
        context?.let {
            AlertDialog.Builder(it)
                    .setMessage("Зарегистрировать аккаунт можно только на сайте.")
                    .setPositiveButton("Регистрация") { dialog, which ->
                        Utils.externalLink("${Api.SITE_URL}/pages/login.php")
                    }
                    .setNeutralButton("Отмена", null)
                    .show()
        }
    }

    override fun onBackPressed(): Boolean {
        presenter.onBackPressed()
        return false
    }

    override fun setRefreshing(refreshing: Boolean) {
        authSwitcher.displayedChild = if (refreshing) 1 else 0
    }

    override fun showSocial(items: List<SocialAuth>) {
        socialAuthAdapter.bindItems(items)
    }

}
