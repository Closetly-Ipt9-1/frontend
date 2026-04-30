package com.m306.closetly.premium.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.m306.closetly.premium.func.shouldWarnAboutExpiry
import com.m306.closetly.premium.func.showPremiumExpiryNotification
import com.m306.closetly.premium.model.PremiumOffer
import com.m306.closetly.premium.model.PremiumPlan
import com.m306.closetly.premium.model.PremiumUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PremiumScreen(viewModel: PremiumViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showPremiumExpiryNotification(context, state.status)
        }
    }

    LaunchedEffect(state.status) {
        if (shouldWarnAboutExpiry(state.status)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showPremiumExpiryNotification(context, state.status)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF17323B),
                        Color(0xFF285A68)
                    )
                )
            )
    ) {
        PremiumContent(
            state = state,
            onBuy = { offer ->
                val activity = context.findActivity()
                if (activity != null) {
                    viewModel.buy(activity, offer)
                }
            },
            onRefresh = viewModel::refresh,
            onDismissMessage = viewModel::clearMessage
        )
    }
}

@Composable
private fun PremiumContent(
    state: PremiumUiState,
    onBuy: (PremiumOffer) -> Unit,
    onRefresh: () -> Unit,
    onDismissMessage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Header()
        }

        if (state.status.isPremium || shouldWarnAboutExpiry(state.status)) {
            item {
                StatusCard(state)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PremiumPlan.entries.forEach { plan ->
                    val offer = state.offers.firstOrNull { it.plan == plan }
                    PlanCard(
                        plan = plan,
                        offer = offer,
                        isCurrentPlan = state.status.planBaseId == plan.basePlanId &&
                            state.status.isPremium,
                        isLoading = state.isLoading,
                        onBuy = onBuy
                    )
                }
            }
        }

        item {
            FeatureComparison()
        }

        item {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Restore purchases")
            }
        }

        state.message?.let { message ->
            item {
                Snackbar(
                    action = {
                        IconButton(onClick = onDismissMessage) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                append("Switch to ")
                withStyle(SpanStyle(color = Color(0xFFFF9800))) {
                    append("Closetly Premium")
                }
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Unlock more storage, outfit tools and an ad-free experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f)
        )
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    offer: PremiumOffer?,
    isCurrentPlan: Boolean,
    isLoading: Boolean,
    onBuy: (PremiumOffer) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.monthlyPrice,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Billed by Google Play",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }

                Button(
                    onClick = { offer?.let(onBuy) },
                    enabled = offer != null && !isLoading && !isCurrentPlan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF162D35)
                    )
                ) {
                    when {
                        isLoading -> CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF162D35)
                        )
                        isCurrentPlan -> Text("Active")
                        offer == null -> Text("Unavailable")
                        else -> Text("Start now")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: PremiumUiState) {
    val status = state.status
    val expiryText = status.estimatedExpiryMillis?.let {
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(it))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF845400)
            )
            Column {
                Text(
                    text = if (status.isPremium) "Premium is active" else "Premium is ending soon",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4E3300),
                    fontWeight = FontWeight.Bold
                )
                if (expiryText != null && !status.isAutoRenewing) {
                    Text(
                        text = "Estimated end date: $expiryText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4E3300)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureComparison() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FeatureColumn(
                title = "Premium",
                features = listOf(
                    "Unlimited clothing items and outfits",
                    "Automatic outfit suggestions",
                    "Advanced categories and tags",
                    "Uploads with background removal",
                    "No ads"
                ),
                titleColor = Color(0xFFFF9800)
            )
            FeatureColumn(
                title = "Free",
                features = listOf(
                    "Up to 6 clothing items",
                    "Maximum 3 outfits",
                    "Basic categories"
                ),
                titleColor = Color.White
            )
        }
    }
}

@Composable
private fun FeatureColumn(
    title: String,
    features: List<String>,
    titleColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = titleColor,
            fontWeight = FontWeight.Bold
        )
        features.forEach { feature ->
            Text(
                text = "• $feature",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.84f)
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
