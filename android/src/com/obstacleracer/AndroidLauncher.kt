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
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration.MAX_AD_CONTENT_RATING_T
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
import java.net.URL


private const val RC_LEADERBOARD_UI = 9004
private const val RC_SIGN_IN = 9005
const val SHARED_PREF = "OBSTACLEDODGE"
private const val SHARED_PREF_HIGHSCORE = "HIGHSCORE"
private const val SHARED_PREF_INVERT = "INVERT"

class AndroidLauncher : AndroidApplication() {
    var adapter: GameAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateConsent()

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

                if (!BuildConfig.DEBUG)
                    createAd()

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
                .setNegativeButton(getString(R.string.edit_consent)) { _, _ -> requestConsentFromUser() }.show())

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                action?.doAction(result.signInAccount!!)
            }
            else{
                Toast.makeText(this,"sign in error ${result.status.statusCode}: ${result.status.statusMessage}"
                        ,Toast.LENGTH_LONG).show()
            }
        }
    }

    fun createAd() {
        return

        MobileAds.setRequestConfiguration(MobileAds.getRequestConfiguration().toBuilder()
                .setMaxAdContentRating(MAX_AD_CONTENT_RATING_T)
                .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE).build())
        MobileAds.initialize(this)

        InterstitialAd(this).apply {
            adUnitId = getString(R.string.admob_ad_id)

            //GDPR bitch
            Log.e("ADMOB", "Loading ad consent: $consent")
            val builder = AdRequest.Builder()
            if (!consent) {
                val extras = Bundle()
                extras.putString("npa", "1")
                builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
            }
            loadAd(builder.build())

            adListener = object : AdListener() {
                override fun onAdLoaded() = show()
                override fun onAdFailedToLoad(p0: Int) {
                    Log.e("ADMOB", "Ad failed to load (code $p0)")
                }
            }
        }
    }

    var consent = false
    private fun updateConsent() {
        return

        val consentInformation = ConsentInformation.getInstance(this)
        val publisherIds = arrayOf(getString(R.string.admob_publisher_id))
        consentInformation.requestConsentInfoUpdate(publisherIds, object : ConsentInfoUpdateListener {
            override fun onFailedToUpdateConsentInfo(reason: String?) {
                consent = false
            }

            override fun onConsentInfoUpdated(consentStatus: ConsentStatus?) {
                when (consentStatus) {
                    ConsentStatus.PERSONALIZED -> consent = true
                    ConsentStatus.NON_PERSONALIZED -> consent = false
                    ConsentStatus.UNKNOWN -> {
                        if (consentInformation.isRequestLocationInEeaOrUnknown) {
                            requestConsentFromUser()
                        } else consent = true
                    }
                }
            }
        })
    }

    var consentForm: ConsentForm? = null
    fun requestConsentFromUser() {
        val privacyUrl = URL(getString(R.string.privacy_policy_url))
        consentForm = ConsentForm.Builder(context, privacyUrl)
                .withListener(object : ConsentFormListener() {
                    override fun onConsentFormLoaded() {
                        consentForm?.show()
                    }

                    override fun onConsentFormOpened() {}

                    override fun onConsentFormClosed(consentStatus: ConsentStatus, userPrefersAdFree: Boolean?) {
                        if (consentStatus == ConsentStatus.PERSONALIZED) {
                            consent = true
                            ConsentInformation.getInstance(context).consentStatus = ConsentStatus.PERSONALIZED
                        } else consent = false
                    }

                    override fun onConsentFormError(errorDescription: String?) {
                        consent = false
                    }
                }).withPersonalizedAdsOption().withNonPersonalizedAdsOption().build()
        consentForm?.load()
    }
}