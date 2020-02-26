package com.dropper

import android.os.Bundle
import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds


class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = AndroidApplicationConfiguration()
        config.numSamples = 5

        initialize(DropperAdapter({ Gdx.files.internal("") }, object : DropperHandler {
            override fun showLeaderboard() {
                TODO("not implemented")
            }

            override fun showRateDialog() = runOnUiThread{
                createRateDialog()
            }

            override fun showAboutDialog() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun submitScore(score: Int) {
                val prefs = getSharedPreferences("SCORE", 0)

                if (score>prefs.getInt("HIGHSCORE",0)){
                    val editor = prefs.edit()
                    editor.putInt("HIGHSCORE",score)
                    editor.apply()
                }

                if (!BuildConfig.DEBUG)
                    createAd()
            }

            override fun getHighscore(): Int {
                val prefs = getSharedPreferences("SCORE", 0)
                return prefs.getInt("HIGHSCORE",0)
            }
        }), config)

        if (rateCondition())
            createRateDialog()
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