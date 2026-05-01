package com.closetly.myapp.explore.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.closetly.myapp.auth.func.AuthManager
import com.closetly.myapp.tags.model.PredefinedTags
import com.closetly.myapp.tags.model.Tag
import androidx.compose.foundation.lazy.LazyRow
import com.closetly.myapp.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

private const val DEBUG_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
private const val RELEASE_NATIVE_AD_UNIT_ID = "ca-app-pub-4262797000632373/3282738680"
private const val OUTFITS_BEFORE_FIRST_AD = 2
private const val OUTFITS_BETWEEN_ADS = 4
private const val PRELOADED_NATIVE_AD_COUNT = 3
private const val EXPLORE_AD_TAG = "ExploreNativeAd"

data class ExploreOutfit(
    val id: String,
    val ownerId: String,
    val username: String,
    val caption: String,
    val imageUrl: String,
    val isPublic: Boolean,
    val likeCount: Int,
    val likedBy: List<String>,
    val saveCount: Int,
    val savedBy: List<String>,
    val createdAt: Long,
    val tags: List<String> = emptyList()
)

data class OutfitComment(
    val id: String,
    val userId: String,
    val username: String,
    val text: String,
    val likeCount: Int,
    val likedBy: List<String>,
    val createdAt: Long
)

@Composable
fun ExploreScreen(onOutfitClick: (String) -> Unit = {}) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val currentUserId = AuthManager.getCurrentUserId()
    val currentUsername = AuthManager.getCurrentUser()?.displayName ?: "Anonymous"
    val nativeAdUnitId = if (BuildConfig.DEBUG) DEBUG_NATIVE_AD_UNIT_ID else RELEASE_NATIVE_AD_UNIT_ID

    var outfits by remember { mutableStateOf<List<ExploreOutfit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var filterTagIds by remember { mutableStateOf<List<String>>(emptyList()) }
    val preloadedNativeAds = remember { mutableStateListOf<NativeAd>() }

    LaunchedEffect(Unit) {
        seedPlaceholderOutfitsIfNeeded(firestore)
    }

    LaunchedEffect(context, nativeAdUnitId) {
        preloadNativeAds(
            context = context,
            adUnitId = nativeAdUnitId,
            targetCount = PRELOADED_NATIVE_AD_COUNT,
            currentCount = { preloadedNativeAds.size },
            onLoaded = { loadedAd ->
                preloadedNativeAds.add(loadedAd)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            preloadedNativeAds.forEach { it.destroy() }
            preloadedNativeAds.clear()
        }
    }

    DisposableEffect(Unit) {
        val listener = firestore
            .collection("outfits")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorText = error.message
                    isLoading = false
                    return@addSnapshotListener
                }

                val loadedOutfits = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        ExploreOutfit(
                            id = document.id,
                            ownerId = document.getString("ownerId") ?: "",
                            username = document.getString("username") ?: "Unknown",
                            caption = document.getString("caption") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            isPublic = document.getBoolean("isPublic") ?: false,
                            likeCount = document.getLong("likeCount")?.toInt() ?: 0,
                            likedBy = (document.get("likedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                            saveCount = document.getLong("saveCount")?.toInt() ?: 0,
                            savedBy = (document.get("savedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                            createdAt = document.getLong("createdAt") ?: 0L,
                            tags = (document.get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty()
                        )
                    }
                    ?.sortedByDescending { it.createdAt }
                    .orEmpty()

                outfits = loadedOutfits
                isLoading = false
                errorText = null
            }

        onDispose {
            listener.remove()
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorText != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = errorText ?: "Etwas ist schiefgelaufen",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        outfits.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Noch keine öffentlichen Outfits",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        else -> {
            val displayedOutfits = if (filterTagIds.isEmpty()) outfits
            else outfits.filter { outfit -> filterTagIds.all { it in outfit.tags } }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ExploreHeader(
                        visibleCount = displayedOutfits.size,
                        selectedTagIds = filterTagIds,
                        onClearTags = { filterTagIds = emptyList() },
                        onTagToggle = { tagId ->
                            filterTagIds = if (tagId in filterTagIds) {
                                filterTagIds - tagId
                            } else {
                                filterTagIds + tagId
                            }
                        }
                    )
                }

                if (displayedOutfits.isEmpty()) {
                    item {
                        Text(
                            text = "Keine Outfits für die ausgewählten Tags.",
                            modifier = Modifier.padding(vertical = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(displayedOutfits, key = { _, outfit -> outfit.id }) { index, outfit ->
                    ExploreOutfitCard(
                        outfit = outfit,
                        currentUserId = currentUserId,
                        currentUsername = currentUsername,
                        firestore = firestore,
                        onCardClick = { onOutfitClick(outfit.id) },
                        onLikeClick = {
                            if (currentUserId != null) {
                                toggleLike(
                                    firestore = firestore,
                                    outfitId = outfit.id,
                                    userId = currentUserId
                                )
                            }
                        },
                        onSaveClick = {
                            if (currentUserId != null) {
                                toggleSave(
                                    firestore = firestore,
                                    outfitId = outfit.id,
                                    userId = currentUserId
                                )
                            }
                        }
                    )

                    if (shouldShowAdAfterOutfit(index)) {
                        val adSlotIndex = adSlotIndexAfterOutfit(index)
                        NativeAdCard(
                            preloadedAd = preloadedNativeAds.getOrNull(adSlotIndex),
                            adUnitId = nativeAdUnitId,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun shouldShowAdAfterOutfit(index: Int): Boolean =
    (index + 1) >= OUTFITS_BEFORE_FIRST_AD &&
        ((index + 1) - OUTFITS_BEFORE_FIRST_AD) % OUTFITS_BETWEEN_ADS == 0

private fun adSlotIndexAfterOutfit(index: Int): Int =
    ((index + 1) - OUTFITS_BEFORE_FIRST_AD) / OUTFITS_BETWEEN_ADS

private fun preloadNativeAds(
    context: Context,
    adUnitId: String,
    targetCount: Int,
    currentCount: () -> Int,
    onLoaded: (NativeAd) -> Unit
) {
    val missingAds = targetCount - currentCount()
    if (missingAds <= 0) return

    repeat(missingAds) {
        AdLoader.Builder(context, adUnitId)
            .forNativeAd { loadedAd ->
                onLoaded(loadedAd)
                Log.d(EXPLORE_AD_TAG, "Preloaded native ad")
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w(
                            EXPLORE_AD_TAG,
                            "Native preload failed: code=${adError.code}, domain=${adError.domain}, message=${adError.message}"
                        )
                    }
                }
            )
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }
}

@Composable
private fun NativeAdCard(
    preloadedAd: NativeAd? = null,
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adFailed by remember { mutableStateOf(false) }
    val adLoader = remember(context, adUnitId) {
        AdLoader.Builder(context, adUnitId)
            .forNativeAd { loadedAd ->
                nativeAd?.destroy()
                nativeAd = loadedAd
                adFailed = false
                Log.d(EXPLORE_AD_TAG, "Native ad loaded")
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        adFailed = true
                        Log.w(
                            EXPLORE_AD_TAG,
                            "Native ad failed to load: code=${adError.code}, domain=${adError.domain}, message=${adError.message}"
                        )
                    }
                }
            )
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
    }

    LaunchedEffect(adLoader, preloadedAd) {
        if (preloadedAd == null) {
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(nativeAd) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    val loadedAd = preloadedAd ?: nativeAd
    if (loadedAd != null && !adFailed) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp),
            factory = { createNativeAdView(it) },
            update = { nativeAdView ->
                populateNativeAdView(loadedAd, nativeAdView)
            }
        )
    } else {
        NativeAdPlaceholder(
            modifier = modifier
        )
    }
}

@Composable
private fun NativeAdPlaceholder(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Anzeige",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gesponserter Platz",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun createNativeAdView(context: Context): NativeAdView {
    val density = context.resources.displayMetrics.density
    fun Int.dpPx(): Int = (this * density).toInt()

    val nativeAdView = NativeAdView(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val container = LinearLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        orientation = LinearLayout.VERTICAL
        setPadding(14.dpPx(), 14.dpPx(), 14.dpPx(), 14.dpPx())
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 24.dpPx().toFloat()
            setColor(0xFFFFFFFF.toInt())
        }
    }

    val badge = TextView(context).apply {
        text = "Anzeige"
        textSize = 12f
        setTextColor(0xFF6F6F6F.toInt())
    }

    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            180.dpPx()
        ).apply {
            topMargin = 10.dpPx()
        }
    }

    val titleRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        setPadding(0, 12.dpPx(), 0, 0)
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(44.dpPx(), 44.dpPx()).apply {
            rightMargin = 10.dpPx()
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val textColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val headlineView = TextView(context).apply {
        textSize = 18f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(0xFF202124.toInt())
    }

    val advertiserView = TextView(context).apply {
        textSize = 13f
        setTextColor(0xFF6F6F6F.toInt())
    }

    val bodyView = TextView(context).apply {
        textSize = 14f
        setTextColor(0xFF3C4043.toInt())
        setPadding(0, 10.dpPx(), 0, 0)
    }

    val callToActionView = Button(context).apply {
        textSize = 14f
        setTextColor(0xFFFFFFFF.toInt())
        backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF6750A4.toInt())
    }

    textColumn.addView(headlineView)
    textColumn.addView(advertiserView)
    titleRow.addView(iconView)
    titleRow.addView(textColumn)

    container.addView(badge)
    container.addView(mediaView)
    container.addView(titleRow)
    container.addView(bodyView)
    container.addView(callToActionView)
    nativeAdView.addView(container)

    nativeAdView.mediaView = mediaView
    nativeAdView.headlineView = headlineView
    nativeAdView.advertiserView = advertiserView
    nativeAdView.iconView = iconView
    nativeAdView.bodyView = bodyView
    nativeAdView.callToActionView = callToActionView

    return nativeAdView
}

private fun populateNativeAdView(
    nativeAd: NativeAd,
    nativeAdView: NativeAdView
) {
    (nativeAdView.headlineView as TextView).text = nativeAd.headline

    nativeAdView.mediaView?.mediaContent = nativeAd.mediaContent

    nativeAdView.bodyView?.visibility = if (nativeAd.body == null) View.GONE else View.VISIBLE
    (nativeAdView.bodyView as TextView).text = nativeAd.body

    nativeAdView.callToActionView?.visibility = if (nativeAd.callToAction == null) View.GONE else View.VISIBLE
    (nativeAdView.callToActionView as Button).text = nativeAd.callToAction

    nativeAdView.iconView?.visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
    (nativeAdView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)

    nativeAdView.advertiserView?.visibility = if (nativeAd.advertiser == null) View.GONE else View.VISIBLE
    (nativeAdView.advertiserView as TextView).text = nativeAd.advertiser

    nativeAdView.setNativeAd(nativeAd)
}

@Composable
private fun ExploreHeader(
    visibleCount: Int,
    selectedTagIds: List<String>,
    onClearTags: () -> Unit,
    onTagToggle: (String) -> Unit
) {
    var filterExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Community-Looks entdecken und nach Stimmung filtern.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "$visibleCount Looks",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.34f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable { filterExpanded = !filterExpanded }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (selectedTagIds.isEmpty()) {
                                "Alle Looks anzeigen"
                            } else {
                                "${selectedTagIds.size} Filter aktiv"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (filterExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (filterExpanded) "Filter schließen" else "Filter öffnen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (filterExpanded) {
                    ExploreFilterChip(
                        label = "Alle",
                        selected = selectedTagIds.isEmpty(),
                        onClick = onClearTags
                    )
                    ExploreTagRow(
                        label = "Saison",
                        tags = PredefinedTags.SEASON,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = onTagToggle
                    )
                    ExploreTagRow(
                        label = "Anlass",
                        tags = PredefinedTags.OCCASION,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = onTagToggle
                    )
                    ExploreTagRow(
                        label = "Stil",
                        tags = PredefinedTags.STYLE,
                        selectedTagIds = selectedTagIds,
                        onTagToggle = onTagToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreTagRow(
    label: String,
    tags: List<Tag>,
    selectedTagIds: List<String>,
    onTagToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag ->
                ExploreFilterChip(
                    label = tag.name,
                    selected = tag.id in selectedTagIds,
                    onClick = { onTagToggle(tag.id) }
                )
            }
        }
    }
}

@Composable
private fun ExploreFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
        )
    }
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun ExploreOutfitCard(
    outfit: ExploreOutfit,
    currentUserId: String?,
    currentUsername: String,
    firestore: FirebaseFirestore,
    onCardClick: () -> Unit = {},
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val isLiked = currentUserId != null && outfit.likedBy.contains(currentUserId)
    val isSaved = currentUserId != null && outfit.savedBy.contains(currentUserId)

    var commentsExpanded by remember { mutableStateOf(true) }
    var showAllComments by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<OutfitComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(outfit.id) {
        val listener = firestore
            .collection("outfits")
            .document(outfit.id)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                comments = snapshot?.documents?.mapNotNull { doc ->
                    OutfitComment(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "Unknown",
                        text = doc.getString("text") ?: "",
                        likeCount = doc.getLong("likeCount")?.toInt() ?: 0,
                        likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.orEmpty()
            }
        onDispose { listener.remove() }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = outfit.username,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (outfit.isPublic) "Öffentlicher Look" else "Privater Look",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = "Community",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .padding(horizontal = 14.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .clickable { onCardClick() },
                contentAlignment = Alignment.Center
            ) {
                if (outfit.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = outfit.imageUrl,
                        contentDescription = outfit.caption.ifBlank { "Outfit" },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Outfit",
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Look Vorschau",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = outfit.caption.ifBlank { "Ohne Beschreibung" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (outfit.caption.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (isLiked) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            IconButton(onClick = onLikeClick) {
                                Icon(
                                    imageVector = if (isLiked) {
                                        Icons.Default.Favorite
                                    } else {
                                        Icons.Default.FavoriteBorder
                                    },
                                    contentDescription = "Like",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isLiked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                text = "${outfit.likeCount}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (isSaved) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            IconButton(onClick = onSaveClick) {
                                Icon(
                                    imageVector = if (isSaved) {
                                        Icons.Default.Check
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = "Save",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSaved) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Text(
                                text = "${outfit.saveCount}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (comments.isEmpty()) "Kommentare" else "${comments.size} Kommentar${if (comments.size != 1) "e" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (commentsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp)
                ) {
                    if (comments.isEmpty()) {
                        Text(
                            text = "Noch keine Kommentare. Sei der Erste!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val visibleComments = if (showAllComments) comments else comments.take(5)
                        visibleComments.forEach { comment ->
                            CommentItem(
                                comment = comment,
                                currentUserId = currentUserId,
                                onLikeClick = {
                                    if (currentUserId != null) {
                                        toggleCommentLike(firestore, outfit.id, comment.id, currentUserId)
                                    }
                                }
                            )
                        }
                        if (comments.size > 5) {
                            Text(
                                text = if (showAllComments) "Weniger anzeigen" else "Weitere ${comments.size - 5} Kommentare anzeigen",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable { showAllComments = !showAllComments }
                            )
                        }
                    }
                }

                if (currentUserId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CommentInputBar(
                        value = commentText,
                        onValueChange = { commentText = it },
                        onSend = {
                            if (commentText.isNotBlank()) {
                                addComment(firestore, outfit.id, currentUserId, currentUsername, commentText.trim())
                                commentText = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 3,
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                text = "Kommentar schreiben",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = if (value.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                },
                contentColor = if (value.isNotBlank()) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                IconButton(onClick = onSend) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Senden",
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: OutfitComment,
    currentUserId: String?,
    onLikeClick: () -> Unit
) {
    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.username,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (comment.likeCount > 0) {
                Text(
                    text = "${comment.likeCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like Kommentar",
                    modifier = Modifier.size(16.dp),
                    tint = if (isLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

