package com.obstacleracer

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val SHARED_PREF_DONT_SHOW_AGAIN = "dontshowagain"
private const val SHARED_PREF_DATE_FIRST_LAUNCH = "date_firstlaunch"
private const val SHARED_PREF_LAUNCH_COUNT = "launch_count"

private const val DAYS_UNTIL_RATE = 3      //Min number of days needed before asking for rating
private const val LAUNCHES_UNTIL_RATE = 3  //Min number of launches before asking for rating

fun Context.rateCondition(): Boolean {
    val prefs = getSharedPreferences(SHARED_PREF, 0)
    if (prefs.getBoolean(SHARED_PREF_DONT_SHOW_AGAIN, false)) return false
    val editor = prefs.edit()

    // Increment launch counter
    val launchCount = prefs.getLong(SHARED_PREF_LAUNCH_COUNT, 0) + 1
    editor.putLong(SHARED_PREF_LAUNCH_COUNT, launchCount)

    // Get date of first launch
    var firstLaunch = prefs.getLong(SHARED_PREF_DATE_FIRST_LAUNCH, 0)
    if (firstLaunch == 0L) {
        firstLaunch = System.currentTimeMillis()
        editor.putLong(SHARED_PREF_DATE_FIRST_LAUNCH, firstLaunch)
    }

    editor.apply()

    val dayCondition = System.currentTimeMillis() >= firstLaunch + DAYS_UNTIL_RATE * 24 * 60 * 60 * 1000
    val countCondition = launchCount >= LAUNCHES_UNTIL_RATE
    return dayCondition && countCondition
}

fun Context.createRateDialog() {
    val editor = getSharedPreferences(SHARED_PREF, 0).edit()
    val dialogClickListener = DialogInterface.OnClickListener { _, which ->
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                editor.putBoolean(SHARED_PREF_DONT_SHOW_AGAIN, true)
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
                editor.putBoolean(SHARED_PREF_DONT_SHOW_AGAIN, true)
                editor.apply()
            }
        }
    }

    MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Material3)
            .setMessage(getString(R.string.rate_message))
            .setTitle(getString(R.string.rate_app))
            .setPositiveButton(getString(R.string.rate), dialogClickListener)
            .setNeutralButton(getString(R.string.later), dialogClickListener)
            .setNegativeButton(getString(R.string.no_thanks), dialogClickListener)
            .show()
}