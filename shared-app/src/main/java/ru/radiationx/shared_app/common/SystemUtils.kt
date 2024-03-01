package ru.radiationx.shared_app.common

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import ru.radiationx.data.downloader.DownloadedFile
import toothpick.InjectConstructor
import java.io.File

@InjectConstructor
class SystemUtils(
    private val context: Context,
) {

    private fun getRemoteFileUri(file: File, name: String): Uri {
        val packageName = context.packageName
        val authority = "${packageName}.remotefileprovider"
        return FileProvider.getUriForFile(context, authority, file, name).also {
            context.grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun openDownloadedFile(file: DownloadedFile) {
        openRemoteFile(file.local, file.remote.name, file.remote.mimeType)
    }

    fun shareDownloadedFile(file: DownloadedFile) {
        shareRemoteFile(file.local, file.remote.name, file.remote.mimeType)
    }

    private fun openRemoteFile(file: File, name: String, mimeType: String) {
        val data = getRemoteFileUri(file, name)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(data, mimeType)
            putExtra(Intent.EXTRA_TITLE, name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        val chooserIntent = Intent.createChooser(intent, "Открыть в").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent);
    }

    private fun shareRemoteFile(file: File, name: String, mimeType: String) {
        val data = getRemoteFileUri(file, name)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, name)
            putExtra(Intent.EXTRA_STREAM, data)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(sendIntent, "Поделиться").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val resolves = context.packageManager.queryIntentActivities(
            chooserIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        resolves.forEach {
            val packageName = it.activityInfo.packageName
            context.grantUriPermission(packageName, data, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooserIntent)
    }

    fun copyToClipBoard(s: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", s)
        clipboard.setPrimaryClip(clip)
    }

    fun readFromClipboard(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val description = clipboard.primaryClipDescription
            val data = clipboard.primaryClip
            if (data != null && description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                return data.getItemAt(0).text.toString()
        }
        return null
    }

    fun shareText(text: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        sendIntent.type = "text/plain"
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(
            Intent.createChooser(sendIntent, "Поделиться").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun externalLink(url: String) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(
            Intent.createChooser(intent, "Открыть в").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

}