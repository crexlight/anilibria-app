package ru.radiationx.anilibria

import android.content.Context
import ru.radiationx.data.datasource.holders.PreferencesHolder
import ru.radiationx.data.migration.MigrationExecutor
import java.io.File
import javax.inject.Inject

class AppMigrationExecutor @Inject constructor(
    private val context: Context,
    private val appPreferences: PreferencesHolder
) : MigrationExecutor {

    override fun execute(current: Int, lastSaved: Int, history: List<Int>) {
        if (current == 20) {
            appPreferences.releaseRemind.value = true
        }
        if (lastSaved <= 52) {
            val dir = File(context.cacheDir, "uil-images")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }
}