package com.obstacleracer

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.view.WindowManager

private const val DONT_SHOW_AGAIN = "dontshowagain"
private const val DATE_FIRST_LAUNCH = "date_firstlaunch"
private const val LAUNCH_COUNT = "launch_count"

private const val DAYS_UNTIL_RATE = 3      //Min number of days needed before asking for rating
private const val LAUNCHES_UNTIL_RATE = 3  //Min number of launches before asking for rating

fun Context.rateCondition(): Boolean {
    val prefs = getSharedPreferences("APP_RATER", 0)
    if (prefs.getBoolean(DONT_SHOW_AGAIN, false)) return false
    val editor = prefs.edit()

    // Increment launch counter
    val launchCount = prefs.getLong(LAUNCH_COUNT, 0) + 1
    editor.putLong(LAUNCH_COUNT, launchCount)

    // Get date of first launch
    var firstLaunch = prefs.getLong(DATE_FIRST_LAUNCH, 0)
    if (firstLaunch == 0L) {
        firstLaunch = System.currentTimeMillis()
        editor.putLong(DATE_FIRST_LAUNCH, firstLaunch)
    }

    editor.apply()

    val dayCondition = System.currentTimeMillis() >= firstLaunch + DAYS_UNTIL_RATE * 24 * 60 * 60 * 1000
    val countCondition = launchCount >= LAUNCHES_UNTIL_RATE
    return dayCondition && countCondition
}

fun Context.createRateDialog() {
    val editor = getSharedPreferences("APP_RATER", 0).edit()
    val dialogClickListener = DialogInterface.OnClickListener { _, which ->
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                editor.putBoolean(DONT_SHOW_AGAIN, true)
                editor.apply()
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                    } else {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                })
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                editor.putBoolean(DONT_SHOW_AGAIN, true)
                editor.apply()
            }
        }
    }

    keepDialog(AlertDialog.Builder(this)
            .setMessage(getString(R.string.rate_message))
            .setTitle(getString(R.string.rate_app))
            .setPositiveButton(getString(R.string.rate), dialogClickListener)
            .setNeutralButton(getString(R.string.later), dialogClickListener)
            .setNegativeButton(getString(R.string.no_thanks), dialogClickListener)
            .show())
}

// Prevent dialog destroy when orientation changes
fun keepDialog(dialog: AlertDialog) = dialog.apply {
    dialog.window?.attributes = WindowManager.LayoutParams().apply {
        copyFrom(dialog.window?.attributes)
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
    }
}