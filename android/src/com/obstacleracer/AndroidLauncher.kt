package com.obstacleracer

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration.MAX_AD_CONTENT_RATING_T
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform


private const val RC_LEADERBOARD_UI = 9004
private const val RC_SIGN_IN = 9005
const val SHARED_PREF = "OBSTACLEDODGE"
private const val SHARED_PREF_HIGHSCORE = "HIGHSCORE"
private const val SHARED_PREF_INVERT = "INVERT"

class AndroidLauncher : AndroidApplication() {
    private var adapter: GameAdapter? = null
    private lateinit var consentInformation: ConsentInformation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        requestConsent {}

        adapter = GameAdapter({ Gdx.files.internal("") }, object : GameHandler {
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

            override fun showSettingsDialog() = runOnUiThread {
                createSettingsDialog()
            }

            override fun submitScore(score: Int) = runOnUiThread {
                val prefs = getSharedPreferences(SHARED_PREF, 0)

                if (score > prefs.getInt(SHARED_PREF_HIGHSCORE, 0)) {
                    val editor = prefs.edit()
                    editor.putInt(SHARED_PREF_HIGHSCORE, score)
                    editor.apply()
                }

                showAdChecked()

                runPlayAction(object : PlayAction {
                    override fun doAction(account: GoogleSignInAccount) = runOnUiThread {
                        submitLeaderboard(account, score.toLong())
                    }
                }, ask = false)
            }

            override fun getHighscore(): Int {
                val prefs = getSharedPreferences(SHARED_PREF, 0)
                return prefs.getInt(SHARED_PREF_HIGHSCORE, 0)
            }
        }, getSharedPreferences(SHARED_PREF, 0).getBoolean(SHARED_PREF_INVERT, false))

        val config = AndroidApplicationConfiguration()
        config.numSamples = 5
        initialize(adapter, config)

        if (rateCondition())
            createRateDialog()
    }

    fun createSettingsDialog() {
        val layout = View.inflate(this, R.layout.dialog_body_settings, null)
        (layout.findViewById<View>(R.id.versionName_view) as TextView).text = try {
            resources.getText(R.string.app_name).toString() + "\n" + getString(R.string.version) + " " +
                    packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            resources.getText(R.string.app_name)
        }

        //Update links
        val textView = layout.findViewById<TextView>(R.id.credit_view)
        textView.movementMethod = LinkMovementMethod.getInstance()

        val switch = layout.findViewById<Switch>(R.id.invert_switch)
        switch.setTextColor(textView.textColors)
        switch.isChecked = getSharedPreferences(SHARED_PREF, 0).getBoolean(SHARED_PREF_INVERT, false)
        switch.setOnCheckedChangeListener { _, requestInverted ->
            val editor = getSharedPreferences(SHARED_PREF, 0).edit()
            editor.putBoolean(SHARED_PREF_INVERT, requestInverted).apply()
            adapter?.setInverted(requestInverted)
        }

        keepDialog(AlertDialog.Builder(this).setView(layout)
                .setPositiveButton(getString(R.string.close), null)
                .setNegativeButton(getString(R.string.edit_consent)) { _, _ -> TODO() }.show()) // TODO

    }

    fun submitLeaderboard(account: GoogleSignInAccount, score: Long) {
        Games.getLeaderboardsClient(this, account)
                .submitScore(getString(R.string.leaderboard_id), score)
    }

    private fun createLeaderboard(account: GoogleSignInAccount) {
        Games.getLeaderboardsClient(this, account)
                .getLeaderboardIntent(getString(R.string.leaderboard_id))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if (result == null) {
                Toast.makeText(this,"sign in error", Toast.LENGTH_LONG).show()
            } else if (result.isSuccess) {
                action?.doAction(result.signInAccount!!)
            } else {
                Toast.makeText(this,"sign in error ${result.status.statusCode}: " +
						"${result.status.statusMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun requestConsent(callback: () -> Unit){
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(this, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { formError ->
                if (formError != null) { // Consent not obtained in current session.
                    Log.e("CONSENT", "Error ${formError.errorCode}: ${formError.message}")
                } else if (consentInformation.canRequestAds()){
                    callback()
                }
            }
        }, { Log.e("CONSENT", "Error ${it.errorCode}: ${it.message}") })
    }

    private fun showAdChecked(){
        if (consentInformation.canRequestAds()) showAd()
        else requestConsent { showAd() }
    }

    private fun showAd() {
        MobileAds.setRequestConfiguration(MobileAds.getRequestConfiguration().toBuilder()
            .setMaxAdContentRating(MAX_AD_CONTENT_RATING_T)
            .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE).build())
        MobileAds.initialize(this)

        val adRequest = AdRequest.Builder().build()
        AdManagerInterstitialAd.load(this, getString(R.string.admob_ad_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("ADMOB", "Ad failed to load (code $adError)")
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.show(this@AndroidLauncher)
                }
            }
        )
    }
}