package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Google AdMob initialization, banner ad configuration,
 * and automatic interstitial pop-up ads across user-provided ad units.
 */
object AdMobManager {
    private const val TAG = "AdMobManager"

    // Primary bottom banner ad unit
    const val BANNER_AD_UNIT_ID = "ca-app-pub-1495262574338316/2803163788"

    // Pool of interstitial pop-up ad units provided by the user
    val INTERSTITIAL_AD_UNIT_IDS = listOf(
        "ca-app-pub-1495262574338316/2903409205",
        "ca-app-pub-1495262574338316/2452733430",
        "ca-app-pub-1495262574338316/8970040322",
        "ca-app-pub-1495262574338316/1182179137",
        "ca-app-pub-1495262574338316/4962596335",
        "ca-app-pub-1495262574338316/2714752655",
        "ca-app-pub-1495262574338316/3503687777"
    )

    // Fallback standard Google test ad units for safety when inventory is pending
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private val isInitialized = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var currentInterstitialIndex = 0
    private var lastAdShownTimestamp: Long = 0L

    // Minimum cooldown between automatic pop-up ads (in milliseconds)
    private const val AD_COOLDOWN_MS = 60_000L // 60 seconds

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context) { status ->
                    Log.d(TAG, "AdMob MobileAds initialized with status: $status")
                    // Pre-load the first interstitial ad so it's ready when needed
                    loadNextInterstitial(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AdMob SDK: ${e.message}", e)
            }
        }
    }

    /**
     * Preloads an interstitial ad using the current rotated ad unit ID.
     */
    fun loadNextInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true

        val adUnitId = INTERSTITIAL_AD_UNIT_IDS[currentInterstitialIndex % INTERSTITIAL_AD_UNIT_IDS.size]
        Log.d(TAG, "Loading Interstitial Ad using unit: $adUnitId (index: $currentInterstitialIndex)")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial Ad successfully loaded ($adUnitId)")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.w(TAG, "Interstitial Ad failed to load from $adUnitId: ${loadAdError.message} (code ${loadAdError.code})")

                    // Cycle to next ad unit ID
                    currentInterstitialIndex = (currentInterstitialIndex + 1) % INTERSTITIAL_AD_UNIT_IDS.size

                    // Attempt fallback to test unit if real units lack fill during staging
                    if (loadAdError.code == 3 /* ERROR_CODE_NO_FILL */) {
                        tryLoadTestInterstitial(context)
                    }
                }
            }
        )
    }

    private fun tryLoadTestInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Test Interstitial Ad loaded as fallback")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    Log.d(TAG, "Test Interstitial Ad also failed: ${loadAdError.message}")
                }
            }
        )
    }

    /**
     * Checks if enough time has passed to show an automatic pop-up ad without spamming the user.
     */
    fun canShowAutomaticAd(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastAdShownTimestamp) >= AD_COOLDOWN_MS
    }

    /**
     * Displays the preloaded interstitial ad if available, and executes onDismissed when done.
     * Automatically preloads the next interstitial once closed.
     */
    fun showInterstitial(activity: Activity, force: Boolean = false, onDismissed: () -> Unit = {}) {
        val currentAd = interstitialAd
        if (currentAd == null || (!force && !canShowAutomaticAd())) {
            // No ad ready or in cooldown; proceed immediately
            if (currentAd == null) {
                loadNextInterstitial(activity)
            }
            onDismissed()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad dismissed")
                interstitialAd = null
                lastAdShownTimestamp = System.currentTimeMillis()
                currentInterstitialIndex = (currentInterstitialIndex + 1) % INTERSTITIAL_AD_UNIT_IDS.size
                loadNextInterstitial(activity)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                interstitialAd = null
                lastAdShownTimestamp = System.currentTimeMillis()
                loadNextInterstitial(activity)
                onDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad showed full screen")
                lastAdShownTimestamp = System.currentTimeMillis()
            }
        }

        try {
            currentAd.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing interstitial: ${e.message}", e)
            interstitialAd = null
            onDismissed()
        }
    }
}
