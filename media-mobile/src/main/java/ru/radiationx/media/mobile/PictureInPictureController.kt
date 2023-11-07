package ru.radiationx.media.mobile

import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.util.Consumer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.radiationx.shared.ktx.android.immutableFlag


class PictureInPictureController(
    private val activity: ComponentActivity,
) {

    private companion object {
        const val ACTION_REMOTE_CONTROL = "action.remote.control"
        const val EXTRA_REMOTE_CONTROL = "extra.remote.control"
    }

    private val modeListener = Consumer<PictureInPictureModeChangedInfo> { mode ->
        _state.update { it.copy(active = mode.isInPictureInPictureMode) }
    }

    private val actionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null || intent.action != ACTION_REMOTE_CONTROL) {
                return
            }
            val extraCode = intent.getIntExtra(EXTRA_REMOTE_CONTROL, Int.MIN_VALUE)
            currentParamsState.actions.find { it.code == extraCode }?.also { action ->
                actionsListener?.invoke(action)
            }
        }
    }

    private var currentParamsState = ParamsState()

    private var _state = MutableStateFlow(
        State(
            supports = isPipSupports(),
            active = isInPictureInPictureMode()
        )
    )
    val state = _state.asStateFlow()

    var actionsListener: ((Action) -> Unit)? = null

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                super.onCreate(owner)
                activity.addOnPictureInPictureModeChangedListener(modeListener)
                registerActionsReceiver()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                activity.removeOnPictureInPictureModeChangedListener(modeListener)
                unregisterActionsReceiver()
            }
        })
    }

    fun enter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupports()) {
            activity.enterPictureInPictureMode(currentParamsState.toParams())
        }
    }

    fun updateParams(block: (ParamsState) -> ParamsState) {
        currentParamsState = block.invoke(currentParamsState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupports()) {
            activity.setPictureInPictureParams(currentParamsState.toParams())
        }
    }

    private fun registerActionsReceiver() {
        val filter = IntentFilter(ACTION_REMOTE_CONTROL)
        val flags = ContextCompat.RECEIVER_EXPORTED
        ContextCompat.registerReceiver(activity, actionsReceiver, filter, flags)
    }

    private fun unregisterActionsReceiver() {
        try {
            activity.unregisterReceiver(actionsReceiver)
        } catch (ignore: Throwable) {
            // do nothing
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ParamsState.toParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
        val remoteActions = actions
            .takeLast(activity.maxNumPictureInPictureActions)
            .map { it.toRemoteAction() }

        builder.setSourceRectHint(sourceHintRect)
        builder.setAspectRatio(aspectRatio)
        builder.setActions(remoteActions)
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun Action.toRemoteAction(): RemoteAction {
        val icon = Icon.createWithResource(activity, icRes)
        val intent = Intent(ACTION_REMOTE_CONTROL).putExtra(EXTRA_REMOTE_CONTROL, code)
        val pendingIntent = PendingIntent.getBroadcast(activity, code, intent, immutableFlag())
        return RemoteAction(icon, title, title, pendingIntent)
    }

    private fun isInPictureInPictureMode(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.isInPictureInPictureMode
        } else {
            false
        }

    private fun isPipSupports(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }
        val hasFeature =
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        val hasPermission = hasPipPermission()
        return hasFeature && hasPermission
    }

    @Suppress("DEPRECATION")
    private fun hasPipPermission(): Boolean {
        val appOps = activity.getSystemService<AppOpsManager>() ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        }
        val op = AppOpsManager.OPSTR_PICTURE_IN_PICTURE
        val pid = android.os.Process.myUid()
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(op, pid, activity.packageName)
        } else {
            appOps.checkOpNoThrow(op, pid, activity.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    data class State(
        val supports: Boolean,
        val active: Boolean,
    )

    data class ParamsState(
        val sourceHintRect: Rect = Rect(),
        val aspectRatio: Rational = Rational(0, 0),
        val actions: List<Action> = emptyList(),
    )

    data class Action(
        val code: Int,
        val title: String,
        @DrawableRes val icRes: Int,
        val important: Boolean,
    )
}