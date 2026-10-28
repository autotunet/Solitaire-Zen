package com.example.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Modern Compose Composable that renders Google AdMob Bottom Banner Ad.
 * Connects to user's Banner Ad Unit ID: ca-app-pub-1495262574338316/2803163788
 */
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobManager.BANNER_AD_UNIT_ID
) {
    var activeAdUnitId by remember(adUnitId) { mutableStateOf(adUnitId) }
    var isAdLoaded by remember { mutableStateOf(false) }
    var currentAdView by remember { mutableStateOf<AdView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, currentAdView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> currentAdView?.pause()
                Lifecycle.Event.ON_RESUME -> currentAdView?.resume()
                Lifecycle.Event.ON_DESTROY -> currentAdView?.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                currentAdView?.destroy()
            } catch (e: Exception) {
                Log.e("AdBannerView", "Error on dispose destroying AdView: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x80000000))
            .navigationBarsPadding()
            .testTag("admob_bottom_banner"),
        contentAlignment = Alignment.Center
    ) {
        key(activeAdUnitId) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admob_banner_view"),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        this.adUnitId = activeAdUnitId
                        currentAdView = this
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                super.onAdLoaded()
                                isAdLoaded = true
                                Log.d("AdBannerView", "Banner ad loaded successfully ($activeAdUnitId)")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                super.onAdFailedToLoad(error)
                                isAdLoaded = false
                                Log.w("AdBannerView", "Banner ad failed to load ($activeAdUnitId): ${error.message} (code ${error.code})")

                                // If real unit has no fill in testing environment, switch active unit to test banner.
                                // AdView does not allow re-setting adUnitId on an existing instance;
                                // updating activeAdUnitId safely triggers key(activeAdUnitId) to instantiate a new AdView.
                                if (error.code == 3 /* NO_FILL */ && activeAdUnitId != AdMobManager.TEST_BANNER_AD_UNIT_ID) {
                                    activeAdUnitId = AdMobManager.TEST_BANNER_AD_UNIT_ID
                                }
                            }
                        }
                        try {
                            loadAd(AdRequest.Builder().build())
                        } catch (e: Exception) {
                            Log.e("AdBannerView", "Error calling loadAd: ${e.message}")
                        }
                    }
                },
                onRelease = { adView ->
                    try {
                        adView.destroy()
                    } catch (e: Exception) {
                        Log.e("AdBannerView", "Error in onRelease destroying AdView: ${e.message}")
                    }
                }
            )
        }
    }
}

