package ru.radiationx.shared_app.analytics.errors

import ru.radiationx.data.analytics.AnalyticsErrorReporter
import timber.log.Timber
import javax.inject.Inject

class LoggingErrorReporter @Inject constructor() : AnalyticsErrorReporter {

    override fun report(message: String, error: Throwable) {
        Timber.tag("LoggingErrorReporter").e(error, message)
    }

    override fun report(group: String, message: String) {
        Timber.tag("LoggingErrorReporter").e("$group -> $message")
    }

    override fun report(group: String, message: String, error: Throwable) {
        Timber.tag("LoggingErrorReporter").e(error, "$group -> $message")
    }
}