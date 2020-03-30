package ru.radiationx.anilibria.screen.update

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_update.*
import permissions.dispatcher.*
import ru.radiationx.anilibria.R
import ru.radiationx.shared.ktx.android.subscribeTo
import ru.radiationx.shared_app.di.viewModel
import ru.radiationx.shared_app.screen.ScopedFragment

@RuntimePermissions
class UpdateFragment : ScopedFragment(R.layout.fragment_update) {

    private val viewModel by viewModel<UpdateViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeTo(viewModel.updateData) {
            val string = StringBuilder().apply {
                appendParam("Версия", it.name.orEmpty())
                appendParam("Дата", it.date.orEmpty())
                appendln("<br>")
                appendSection("Важно", it.important)
                appendSection("Добавлено", it.added)
                appendSection("Исправлено", it.fixed)
                appendSection("Изменено", it.changed)
            }
            updateDescription.text = Html.fromHtml(string.toString())
        }

        updateButton.setOnClickListener { onUpdateClickWithPermissionCheck() }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onUpdateClick() {
        viewModel.onUpdateClick()
    }

    @OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onNeverAskAgain() {
        AlertDialog.Builder(requireContext())
            .setMessage("Для загрузки обновления необходим доступ к памяти")
            .setPositiveButton("Настройки") { dialog, which ->
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", requireActivity().packageName, null)
                }
                requireActivity().startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun StringBuilder.appendParam(title: String, value: String) {
        append("<b>$title:</b> $value<br>")
    }

    private fun StringBuilder.appendSection(title: String, changes: List<String>) {
        if (changes.isEmpty()) {
            return
        }
        append("<b>$title</b><br>")
        changes.forEachIndexed { index, s ->
            append("— ").append(s)
            if (index + 1 < changes.size) {
                append("<br>")
            }
        }
        append("<br>")
    }

}