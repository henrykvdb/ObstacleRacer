package com.obstacleracer

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.widget.TextView
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games


private const val RC_LEADERBOARD_UI = 9004
private const val RC_SIGN_IN = 9005

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration()
        config.numSamples = 5

        initialize(GameAdapter({ Gdx.files.internal("") }, object : GameHandler {
            override fun showLeaderboard() {
                runPlayAction(object : PlayAction {
                    override fun doAction(account: GoogleSignInAccount) = runOnUiThread {
                        createLeaderboard(account)
                    }
                }, ask = true)
            }

            override fun showRateDialog() = runOnUiThread {
                createRateDialog()
            }

            override fun showAboutDialog() = runOnUiThread {
                createAboutDialog()
            }

            override fun submitScore(score: Int) {
                val prefs = getSharedPreferences("SCORE", 0)

                if (score > prefs.getInt("HIGHSCORE", 0)) {
                    val editor = prefs.edit()
                    editor.putInt("HIGHSCORE", score)
                    editor.apply()
                }

                if (!BuildConfig.DEBUG)
                    createAd()

                runPlayAction(object : PlayAction {
                    override fun doAction(account: GoogleSignInAccount) = runOnUiThread {
                        submitLeaderboard(account, score.toLong())
                    }
                }, ask = false)
            }

            override fun getHighscore(): Int {
                val prefs = getSharedPreferences("SCORE", 0)
                return prefs.getInt("HIGHSCORE", 0)
            }
        }), config)

        if (rateCondition())
            createRateDialog()
    }

    fun createAboutDialog() {
        val layout = View.inflate(this, R.layout.dialog_body_about, null)

        (layout.findViewById<View>(R.id.versionName_view) as TextView).text = try {
            resources.getText(R.string.app_name).toString() + "\n" + getString(R.string.version) + " " +
                    packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            resources.getText(R.string.app_name)
        }

        //Update links
        val textView = layout.findViewById<TextView>(R.id.license_view)
        textView.movementMethod = LinkMovementMethod.getInstance()

        keepDialog(AlertDialog.Builder(this).setView(layout).setPositiveButton("close", null).show())

    }

    fun submitLeaderboard(account: GoogleSignInAccount, score: Long) {
        Games.getLeaderboardsClient(this, account)
                .submitScore("CgkIyOWB2csHEAIQAA", score)
    }

    private fun createLeaderboard(account: GoogleSignInAccount) {
        Games.getLeaderboardsClient(this, account)
                .getLeaderboardIntent("CgkIyOWB2csHEAIQAA")
                .addOnSuccessListener { intent ->
                    startActivityForResult(intent, RC_LEADERBOARD_UI)
                }
    }

    interface PlayAction {
        fun doAction(account: GoogleSignInAccount)
    }

    private var action: PlayAction? = null
    private fun runPlayAction(action: PlayAction, ask: Boolean) {
        this.action = action
        val signInOptions = GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        val account = GoogleSignIn.getLastSignedInAccount(this)

        // Already signed in.
        if (GoogleSignIn.hasPermissions(account, *signInOptions.scopeArray)) {
            action.doAction(account!!)
        } else {
            // Try Silent sign-in first.
            GoogleSignIn.getClient(this, signInOptions).silentSignIn().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    action.doAction(task.result!!)
                } else if (ask) {
                    // Try Sign in pop-up
                    val intent = GoogleSignIn.getClient(this,
                            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).signInIntent
                    startActivityForResult(intent, RC_SIGN_IN)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                action?.doAction(result.signInAccount!!)
            }
        }
    }

    fun createAd() {
        MobileAds.initialize(this)
        InterstitialAd(this).apply {
            adUnitId = getString(R.string.admob_ad_id)
            loadAd(AdRequest.Builder().build())
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    show()
                }

                override fun onAdFailedToLoad(p0: Int) {
                    Log.e("ADMOB", "Ad failed to load (code $p0)")
                }
            }
        }
    }
}