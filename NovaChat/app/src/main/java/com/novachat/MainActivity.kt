package com.novachat

import android.Manifest
import android.app.role.RoleManager
import com.novachat.BuildConfig
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.novachat.core.billing.LicenseManager
import com.novachat.core.datastore.UserPreferencesRepository
import com.novachat.core.sms.BubbleNotificationHelper
import com.novachat.core.theme.NovaChatMaterialTheme
import com.novachat.core.worker.ScheduledMessageWorker
import com.novachat.core.worker.SpamLearningDecayWorker
import com.novachat.domain.model.BubbleShape
import com.novachat.domain.model.NovaChatTheme
import com.novachat.domain.repository.ConversationRepository
import com.novachat.domain.repository.ThemeRepository
import com.novachat.ui.navigation.ChatRoute
import com.novachat.ui.navigation.NovaChatNavHost
import com.novachat.ui.onboarding.RestoreOnboardingScreen
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bubbleNotificationHelper: BubbleNotificationHelper

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var themeRepository: ThemeRepository

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var licenseManager: LicenseManager

    private var hasPermissions by mutableStateOf(false)
    private var pendingChatThreadId by mutableStateOf(-1L)
    private var pendingChatAddress by mutableStateOf<String?>(null)
    private var pendingChatContactName by mutableStateOf<String?>(null)

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.RECEIVE_MMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val defaultSmsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requestPermissionsIfNeeded()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.entries.all { it.value }
    }

    /**
     * Google Play policy requires the default handler prompt to appear
     * before any runtime permission requests. This method enforces that order:
     * default SMS role prompt first, then runtime permissions in its callback.
     */
    private fun startSetupFlow() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
            requestPermissionsIfNeeded()
        } else {
            defaultSmsLauncher.launch(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            )
        }
    }

    private fun requestPermissionsIfNeeded() {
        hasPermissions = requiredPermissions
            .all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { provider ->
            provider.remove()
        }
        enableEdgeToEdge()

        hasPermissions = requiredPermissions
            .all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

        if (!hasPermissions) {
            startSetupFlow()
        }

        handleNotificationIntent(intent)
        bubbleNotificationHelper.createBubbleChannel()
        ScheduledMessageWorker.enqueue(this)
        SpamLearningDecayWorker.enqueue(this)

        setContent {
            val themeMode by userPreferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = "system")
            val activeThemeId by userPreferencesRepository.activeThemeId
                .collectAsStateWithLifecycle(initialValue = 1L)
            val bubbleShapeOverride by userPreferencesRepository.activeBubbleShape
                .collectAsStateWithLifecycle(initialValue = null)
            val conversationBackgroundId by userPreferencesRepository.conversationBackgroundId
                .collectAsStateWithLifecycle(initialValue = "default")
            var activeTheme by remember { mutableStateOf<NovaChatTheme?>(null) }
            val conversationBackgroundOverride = remember(conversationBackgroundId) {
                com.novachat.domain.model.BuiltInBackgrounds.findById(conversationBackgroundId)
            }
            val useSystemTheme = themeMode == "system"

            LaunchedEffect(activeThemeId) {
                themeRepository.seedBuiltInThemes()
                activeTheme = themeRepository.getThemeById(activeThemeId)
            }

            NovaChatMaterialTheme(
                activeTheme = if (useSystemTheme) null else activeTheme,
                useDynamicColor = useSystemTheme,
                bubbleShapeOverride = bubbleShapeOverride,
                conversationBackgroundOverride = conversationBackgroundOverride
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isFirstLaunch by userPreferencesRepository.isFirstLaunch
                        .collectAsStateWithLifecycle(initialValue = false)
                    val scope = rememberCoroutineScope()

                    if (!hasPermissions) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Permissions Required",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aura needs SMS and contacts permissions to function as your messaging app.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { startSetupFlow() }) {
                                Text("Grant Permissions")
                            }
                        }
                    } else if (isFirstLaunch) {
                        RestoreOnboardingScreen(
                            onSkip = {
                                scope.launch {
                                    userPreferencesRepository.setFirstLaunchComplete()
                                }
                            }
                        )
                    } else {
                        val navController = rememberNavController()

                        LaunchedEffect(pendingChatThreadId, pendingChatAddress) {
                            val addr = pendingChatAddress
                            if (addr != null) {
                                var tid = pendingChatThreadId
                                if (tid <= 0) {
                                    tid = conversationRepository.getThreadIdForAddress(addr)
                                }
                                if (tid > 0) {
                                    navController.navigate(
                                        ChatRoute(tid, addr, pendingChatContactName)
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                                pendingChatThreadId = -1L
                                pendingChatAddress = null
                                pendingChatContactName = null
                            }
                        }

                        NovaChatNavHost(navController = navController)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        licenseManager.recheckLicense()
        maybePromptReview()
    }

    private fun maybePromptReview() {
        lifecycleScope.launch {
            userPreferencesRepository.setInstallTimeIfNeeded()
            val alreadyShown = userPreferencesRepository.reviewShown.first()
            if (alreadyShown) return@launch
            val installTime = userPreferencesRepository.installTime.first()
            if (installTime == 0L) return@launch
            val threeDaysMs = 3L * 24 * 60 * 60 * 1000
            if (System.currentTimeMillis() - installTime < threeDaysMs) return@launch

            val reviewManager = ReviewManagerFactory.create(this@MainActivity)
            reviewManager.requestReviewFlow().addOnSuccessListener { reviewInfo ->
                reviewManager.launchReviewFlow(this@MainActivity, reviewInfo)
                    .addOnCompleteListener {
                        lifecycleScope.launch {
                            userPreferencesRepository.setReviewShown()
                        }
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent ?: return
        val threadId = intent.getLongExtra("threadId", -1L)
        val address = intent.getStringExtra("address")
        if (BuildConfig.DEBUG) android.util.Log.d("NC_DEBUG", "+++ MainActivity.handleNotificationIntent threadId=$threadId address=$address")
        if (address != null) {
            pendingChatThreadId = if (threadId > 0) threadId else 0L
            pendingChatAddress = address
            pendingChatContactName = intent.getStringExtra("contactName")
            if (BuildConfig.DEBUG) android.util.Log.d("NC_DEBUG", "+++ MainActivity: set pending tid=$pendingChatThreadId addr=$pendingChatAddress")
            intent.removeExtra("threadId")
            intent.removeExtra("address")
            intent.removeExtra("contactName")
        }
    }
}
