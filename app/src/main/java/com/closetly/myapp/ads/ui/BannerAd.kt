package com.closetly.myapp.ads.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
private const val BANNER_AD_TAG = "ClosetlyBannerAd"

private enum class AdLoadState {
    Loading,
    Loaded,
    Failed
}

@Composable
fun BannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val adSize = remember(context, screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    var loadState by remember(context, screenWidthDp) { mutableStateOf(AdLoadState.Loading) }
    val adView = remember(context, screenWidthDp) {
        AdView(context).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    loadState = AdLoadState.Loaded
                    Log.d(BANNER_AD_TAG, "Banner ad loaded")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    loadState = AdLoadState.Failed
                    Log.w(BANNER_AD_TAG, "Banner ad failed to load: ${adError.message}")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { adView }
        )

        if (loadState != AdLoadState.Loaded) {
            Text(
                text = "Anzeige",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
