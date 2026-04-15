package com.novachat.ui.financial

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novachat.R
import java.util.Locale

@Composable
fun FinancialOnboardingScreen(
    onComplete: () -> Unit,
    viewModel: FinancialOnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val selectedProviders by viewModel.selectedProviders.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Premium badge
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFC107)
            ) {
                Text(
                    text = stringResource(R.string.financial_onboarding_badge),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Step indicator dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .background(
                            if (index == currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Step content
        Box(modifier = Modifier.weight(1f)) {
            val providers = detectCountryProviders()

            when (currentStep) {
                0 -> Step1FeatureIntro()
                1 -> Step2ProviderLinks(
                    selectedProviders = selectedProviders,
                    onToggleProvider = viewModel::toggleProvider
                )
                2 -> Step3ConfirmEnable(
                    onComplete = { scanInbox ->
                        viewModel.completeOnboarding(providers, scanInbox)
                        onComplete()
                    }
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 0) {
                OutlinedButton(onClick = viewModel::previousStep) {
                    Text(stringResource(R.string.back))
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (currentStep < 2) {
                Button(onClick = viewModel::nextStep) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}

@Composable
private fun Step1FeatureIntro() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.financial_intelligence),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.financial_onboarding_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4CAF50).copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.financial_onboarding_privacy_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun detectCountryProviders(): List<FinancialProvider> {
    val context = LocalContext.current
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val simCountry = tm?.simCountryIso?.uppercase()
    val locale = Locale.getDefault()

    val isIsrael = simCountry == "IL"
            || locale.language in listOf("he", "iw")
            || locale.country == "IL"
    val isUS = !isIsrael && (simCountry == "US" || locale.country == "US")

    return when {
        isIsrael -> FinancialProviders.israeliProviders
        isUS -> FinancialProviders.usProviders
        else -> emptyList()
    }
}

@Composable
private fun Step2ProviderLinks(
    selectedProviders: Set<String>,
    onToggleProvider: (String) -> Unit
) {
    val context = LocalContext.current
    val providers = detectCountryProviders()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.financial_onboarding_step2_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.financial_onboarding_step2_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (providers.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(providers) { provider ->
                    ProviderRow(
                        provider = provider,
                        isSelected = provider.smsAddress in selectedProviders,
                        onToggle = { onToggleProvider(provider.smsAddress) },
                        onOpenSetup = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(provider.url))
                            )
                        }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.financial_onboarding_no_providers_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = stringResource(R.string.financial_onboarding_manual_add_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ProviderRow(
    provider: FinancialProvider,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onOpenSetup: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(provider.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = provider.abbreviation,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = provider.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onOpenSetup) {
                Text(stringResource(R.string.open_setup))
            }
        }
    }
}

@Composable
private fun Step3ConfirmEnable(
    onComplete: (scanInbox: Boolean) -> Unit
) {
    var smsEnabled by remember { mutableStateOf(false) }
    var featureEnabled by remember { mutableStateOf(true) }
    var scanInbox by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.youre_all_set),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = smsEnabled,
                onCheckedChange = { smsEnabled = it }
            )
            Text(
                text = stringResource(R.string.financial_confirm_sms),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = scanInbox,
                onCheckedChange = { scanInbox = it }
            )
            Column {
                Text(
                    text = stringResource(R.string.financial_confirm_scan_inbox),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.financial_confirm_scan_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.enable_financial_intelligence),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = featureEnabled,
                onCheckedChange = { featureEnabled = it }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onComplete(scanInbox) },
            enabled = smsEnabled && featureEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.get_started),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
