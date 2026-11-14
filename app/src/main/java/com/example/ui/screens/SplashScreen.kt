package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.auth.AccessCodeEntry
import com.example.data.auth.AppUserRole
import com.example.data.auth.EmailIdentityResolution
import com.example.data.auth.UserSession
import com.example.ui.theme.BrandCyanPrimary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDarkCardElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.MikroTikCyan
import com.example.ui.theme.SemanticExpenseRed
import com.example.ui.theme.SemanticIncomeGreen
import com.example.ui.theme.SemanticWarningAmber
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay

/**
 * Deterministic routing outcome for the Splash Role Selection Gateway & Persistent Auto-Login.
 */
enum class SplashGatewayDestination {
    SHOW_ROLE_SELECTION_GATEWAY,
    AUTO_ROUTE_OWNER,
    AUTO_ROUTE_AGENT,
    AUTO_ROUTE_POS
}

fun resolveSplashGatewayDestination(
    isOnboardingComplete: Boolean,
    currentUser: UserSession?,
    activeRole: AppUserRole
): SplashGatewayDestination {
    if (!isOnboardingComplete || currentUser == null) {
        return SplashGatewayDestination.SHOW_ROLE_SELECTION_GATEWAY
    }
    return when (activeRole) {
        AppUserRole.NETWORK_OWNER -> SplashGatewayDestination.AUTO_ROUTE_OWNER
        AppUserRole.DISTRIBUTOR_AGENT -> SplashGatewayDestination.AUTO_ROUTE_AGENT
        AppUserRole.POS_MERCHANT -> SplashGatewayDestination.AUTO_ROUTE_POS
    }
}

/**
 * Splash Screen & First-Time Role Selection Gateway (`بوابة اختيار الدور والهوية السحابية`).
 *
 * - When an authenticated session is already persisted (`!isFirstTimeOrLoggedOut`):
 *   Automatically routes the user straight to their active workspace (`NETWORK_OWNER`, `DISTRIBUTOR_AGENT`, or `POS_MERCHANT`)
 *   once the initialization animation finishes.
 * - When no session is active (`isFirstTimeOrLoggedOut`):
 *   Smoothly reveals the 3 role selection cards at the bottom of the screen and guides the user through
 *   first-time Email Binding + Cloud Identity Recovery (`users_by_email/{sanitizedEmail}`).
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    viewModel: AppViewModel? = null,
    skipAnimation: Boolean = false,
    onRoleCompleted: (AppUserRole) -> Unit = { onSplashFinished() },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnboardingComplete by if (viewModel != null) {
        viewModel.isOnboardingComplete.collectAsState()
    } else {
        remember { mutableStateOf(true) }
    }
    val currentUser by if (viewModel != null) {
        viewModel.currentUser.collectAsState()
    } else {
        remember { mutableStateOf<UserSession?>(null) }
    }
    val activeRole by if (viewModel != null) {
        viewModel.activeRole.collectAsState()
    } else {
        remember { mutableStateOf(AppUserRole.NETWORK_OWNER) }
    }
    val savedAccessCodes by if (viewModel != null) {
        viewModel.accessCodes.collectAsState()
    } else {
        remember { mutableStateOf<List<AccessCodeEntry>>(emptyList()) }
    }

    val isFirstTimeOrLoggedOut = viewModel != null && (!isOnboardingComplete || currentUser == null)

    val scale = remember { Animatable(if (skipAnimation) 1f else 0.85f) }
    val alpha = remember { Animatable(if (skipAnimation) 1f else 0f) }
    val textAlpha = remember { Animatable(if (skipAnimation) 1f else 0f) }
    val progress = remember { Animatable(if (skipAnimation) 1f else 0f) }

    var isInitComplete by rememberSaveable { mutableStateOf(skipAnimation) }
    var selectedGatewayRole by rememberSaveable { mutableStateOf<AppUserRole?>(null) }

    // Onboarding inputs
    var emailInput by rememberSaveable { mutableStateOf("") }
    var displayNameInput by rememberSaveable { mutableStateOf("") }
    var agentCodeInput by rememberSaveable { mutableStateOf("") }
    var agentPinInput by rememberSaveable { mutableStateOf("") }
    var posCodeInput by rememberSaveable { mutableStateOf("") }
    var gatewayError by rememberSaveable { mutableStateOf<String?>(null) }
    var gatewayNotice by rememberSaveable { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    LaunchedEffect(skipAnimation) {
        if (!skipAnimation) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(skipAnimation) {
        if (!skipAnimation) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = LinearEasing)
            )
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(skipAnimation, isFirstTimeOrLoggedOut) {
        if (!skipAnimation) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
            delay(120)
        }
        isInitComplete = true
        val destination = resolveSplashGatewayDestination(
            isOnboardingComplete = isOnboardingComplete,
            currentUser = currentUser,
            activeRole = activeRole
        )
        if (viewModel == null || destination != SplashGatewayDestination.SHOW_ROLE_SELECTION_GATEWAY) {
            onRoleCompleted(activeRole)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF061426),
                        Color(0xFF041E26)
                    )
                )
            )
            .testTag("screen_splash"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background blur circles
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(glowScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandCyanPrimary.copy(alpha = 0.25f),
                            Color(0xFF10B981).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Logo Container with sleek neon border
            Surface(
                modifier = Modifier
                    .size(if (isInitComplete && isFirstTimeOrLoggedOut) 96.dp else 136.dp)
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .testTag("splash_logo_surface"),
                shape = RoundedCornerShape(28.dp),
                color = Color.Black,
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BrandCyanPrimary,
                            Color(0xFF10B981),
                            Color(0xFF2563EB)
                        )
                    )
                ),
                shadowElevation = 18.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "شعار سام ميكروتك الرسمي",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Name & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value)
            ) {
                Text(
                    text = "سام ميكروتك",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (isInitComplete && isFirstTimeOrLoggedOut) 24.sp else 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "تحكم بشبكتك وحساباتك المالية وفق أعلى المعايير المحاسبية",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF67E8F9),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SplashPill(text = "إدارة ميكروتك ⚡")
                    SplashPill(text = "معايير IFRS 📊")
                    SplashPill(text = "كروت ذكية 🎨")
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Smooth Progress Indicator while initializing
            if (!isInitComplete) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(180.dp)
                        .alpha(textAlpha.value)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = BrandCyanPrimary,
                        trackColor = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "جاري تهيئة النظام والمزامنة...",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // First-Time Role Selection Gateway (shown when no session is saved)
            AnimatedVisibility(
                visible = isInitComplete && isFirstTimeOrLoggedOut,
                enter = fadeIn(tween(400)) + expandVertically(tween(400)),
                exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
            ) {
                if (selectedGatewayRole == null) {
                    SplashRoleSelectionGatewayCards(
                        onSelectRole = { role ->
                            selectedGatewayRole = role
                            gatewayError = null
                            gatewayNotice = null
                        },
                        onQuickCloudEmailRecover = {
                            if (viewModel != null) {
                                viewModel.signInWithGoogleForRoleGateway(
                                    context = context,
                                    selectedRole = null,
                                    onResult = { resolution ->
                                        when (resolution) {
                                            is EmailIdentityResolution.RestoredFromCloud -> {
                                                onRoleCompleted(resolution.session.role)
                                            }
                                            is EmailIdentityResolution.NewOwnerSessionCreated -> {
                                                onRoleCompleted(AppUserRole.NETWORK_OWNER)
                                            }
                                            is EmailIdentityResolution.RequiresRoleCode -> {
                                                selectedGatewayRole = resolution.role
                                                emailInput = resolution.email
                                            }
                                        }
                                    },
                                    onError = { err ->
                                        gatewayError = err
                                    }
                                )
                            }
                        },
                        errorMessage = gatewayError
                    )
                } else {
                    SplashRoleEmailOnboardingCard(
                        role = selectedGatewayRole ?: AppUserRole.NETWORK_OWNER,
                        emailInput = emailInput,
                        onEmailChange = {
                            emailInput = it
                            gatewayError = null
                        },
                        displayNameInput = displayNameInput,
                        onDisplayNameChange = {
                            displayNameInput = it
                            gatewayError = null
                        },
                        agentCodeInput = agentCodeInput,
                        onAgentCodeChange = {
                            agentCodeInput = it.uppercase()
                            gatewayError = null
                        },
                        agentPinInput = agentPinInput,
                        onAgentPinChange = {
                            agentPinInput = it
                            gatewayError = null
                        },
                        posCodeInput = posCodeInput,
                        onPosCodeChange = {
                            posCodeInput = it.uppercase()
                            gatewayError = null
                        },
                        availableAgentCodes = savedAccessCodes.filter {
                            it.role == AppUserRole.DISTRIBUTOR_AGENT && it.isActive
                        },
                        availablePosCodes = savedAccessCodes.filter {
                            it.role == AppUserRole.POS_MERCHANT && it.isActive
                        },
                        errorMessage = gatewayError,
                        noticeMessage = gatewayNotice,
                        onBackToRoles = {
                            selectedGatewayRole = null
                            gatewayError = null
                            gatewayNotice = null
                        },
                        onGoogleOneTapRecoverOrBind = {
                            val role = selectedGatewayRole ?: AppUserRole.NETWORK_OWNER
                            viewModel?.signInWithGoogleForRoleGateway(
                                context = context,
                                selectedRole = role,
                                onResult = { resolution ->
                                    when (resolution) {
                                        is EmailIdentityResolution.RestoredFromCloud -> {
                                            onRoleCompleted(resolution.session.role)
                                        }
                                        is EmailIdentityResolution.NewOwnerSessionCreated -> {
                                            onRoleCompleted(AppUserRole.NETWORK_OWNER)
                                        }
                                        is EmailIdentityResolution.RequiresRoleCode -> {
                                            emailInput = resolution.email
                                            gatewayNotice = "✅ تم التحقق من البريد (${resolution.email}). أدخل الكود لربطه بحسابك السحابي."
                                        }
                                    }
                                },
                                onError = { err ->
                                    gatewayError = err
                                }
                            )
                        },
                        onCheckEmailCloudRecovery = {
                            val cleanEmail = emailInput.trim()
                            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                gatewayError = "يرجى إدخال البريد الإلكتروني أولاً للتحقق السحابي"
                                return@SplashRoleEmailOnboardingCard
                            }
                            viewModel?.recoverOrBindEmailIdentity(
                                email = cleanEmail,
                                displayName = displayNameInput.trim(),
                                selectedRole = selectedGatewayRole,
                                onResult = { resolution ->
                                    when (resolution) {
                                        is EmailIdentityResolution.RestoredFromCloud -> {
                                            onRoleCompleted(resolution.session.role)
                                        }
                                        is EmailIdentityResolution.NewOwnerSessionCreated -> {
                                            onRoleCompleted(AppUserRole.NETWORK_OWNER)
                                        }
                                        is EmailIdentityResolution.RequiresRoleCode -> {
                                            gatewayNotice = "لا يوجد كود مرتبط سابقاً بهذا البريد. أدخل كود الدور أدناه لربطه لأول مرة."
                                        }
                                    }
                                },
                                onError = { err ->
                                    gatewayError = err
                                }
                            )
                        },
                        onSubmitRoleOnboarding = {
                            val role = selectedGatewayRole ?: AppUserRole.NETWORK_OWNER
                            val cleanEmail = emailInput.trim()
                            when (role) {
                                AppUserRole.NETWORK_OWNER -> {
                                    if (cleanEmail.isNotBlank() && !cleanEmail.contains("@")) {
                                        gatewayError = "يرجى إدخال بريد إلكتروني صحيح أو المتابعة المباشرة"
                                        return@SplashRoleEmailOnboardingCard
                                    }
                                    if (cleanEmail.isNotBlank()) {
                                        viewModel?.recoverOrBindEmailIdentity(
                                            email = cleanEmail,
                                            displayName = displayNameInput.trim(),
                                            selectedRole = AppUserRole.NETWORK_OWNER,
                                            onResult = {
                                                onRoleCompleted(AppUserRole.NETWORK_OWNER)
                                            },
                                            onError = { err ->
                                                gatewayError = err
                                            }
                                        )
                                    } else {
                                        viewModel?.completeOwnerOnboarding(
                                            email = "",
                                            displayName = displayNameInput.trim()
                                        ) {
                                            onRoleCompleted(AppUserRole.NETWORK_OWNER)
                                        } ?: onRoleCompleted(AppUserRole.NETWORK_OWNER)
                                    }
                                }
                                AppUserRole.DISTRIBUTOR_AGENT -> {
                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                        gatewayError = "يرجى إدخال البريد الإلكتروني لربط حساب الوكيل واسترجاعه عند تغيير الهاتف"
                                        return@SplashRoleEmailOnboardingCard
                                    }
                                    // If code is blank, try smart cloud recovery by email first!
                                    if (agentCodeInput.isBlank()) {
                                        viewModel?.recoverOrBindEmailIdentity(
                                            email = cleanEmail,
                                            displayName = agentPinInput.trim(),
                                            selectedRole = AppUserRole.DISTRIBUTOR_AGENT,
                                            onResult = { resolution ->
                                                when (resolution) {
                                                    is EmailIdentityResolution.RestoredFromCloud -> {
                                                        onRoleCompleted(resolution.session.role)
                                                    }
                                                    else -> {
                                                        gatewayError = "يرجى إدخال كود دعوة الوكيل (AGT-XXXX) لربطه بالبريد لأول مرة"
                                                    }
                                                }
                                            },
                                            onError = { err -> gatewayError = err }
                                        )
                                    } else {
                                        viewModel?.loginWithAgentInviteCode(
                                            codeInput = agentCodeInput,
                                            nameOrPinInput = agentPinInput,
                                            emailInput = cleanEmail,
                                            onSuccess = {
                                                onRoleCompleted(AppUserRole.DISTRIBUTOR_AGENT)
                                            },
                                            onError = { err ->
                                                gatewayError = err
                                            }
                                        )
                                    }
                                }
                                AppUserRole.POS_MERCHANT -> {
                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                        gatewayError = "يرجى إدخال البريد الإلكتروني لربط بوابة نقطة البيع واسترجاعها تلقائياً"
                                        return@SplashRoleEmailOnboardingCard
                                    }
                                    if (posCodeInput.isBlank()) {
                                        viewModel?.recoverOrBindEmailIdentity(
                                            email = cleanEmail,
                                            displayName = displayNameInput.trim(),
                                            selectedRole = AppUserRole.POS_MERCHANT,
                                            onResult = { resolution ->
                                                when (resolution) {
                                                    is EmailIdentityResolution.RestoredFromCloud -> {
                                                        onRoleCompleted(resolution.session.role)
                                                    }
                                                    else -> {
                                                        gatewayError = "يرجى إدخال كود الشفافية الرقمية (POS-XXXXXX) لربطه بالبريد لأول مرة"
                                                    }
                                                }
                                            },
                                            onError = { err -> gatewayError = err }
                                        )
                                    } else {
                                        viewModel?.loginWithPosTransparencyCode(
                                            codeInput = posCodeInput,
                                            emailInput = cleanEmail,
                                            onSuccess = {
                                                onRoleCompleted(AppUserRole.POS_MERCHANT)
                                            },
                                            onError = { err ->
                                                gatewayError = err
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashRoleSelectionGatewayCards(
    onSelectRole: (AppUserRole) -> Unit,
    onQuickCloudEmailRecover: () -> Unit,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("splash_role_gateway_container"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "اختر صفتك للدخول وتخصيص بيئة العمل (تُحفظ تلقائياً):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        // Role 1: NETWORK_OWNER
        SplashRoleGatewayCard(
            title = "👑 مالك الشبكة",
            subtitle = "إدارة الميكروتك والنظام المحاسبي الشامل",
            accentColor = BrandCyanPrimary,
            icon = Icons.Default.Router,
            tag = "splash_role_card_owner",
            onClick = { onSelectRole(AppUserRole.NETWORK_OWNER) }
        )

        // Role 2: DISTRIBUTOR_AGENT
        SplashRoleGatewayCard(
            title = "🚚 وكيل / موزع ميداني",
            subtitle = "إدارة التوزيع والفواتير والتحصيل",
            accentColor = Color(0xFF38BDF8),
            icon = Icons.Default.LocalShipping,
            tag = "splash_role_card_agent",
            onClick = { onSelectRole(AppUserRole.DISTRIBUTOR_AGENT) }
        )

        // Role 3: POS_MERCHANT
        SplashRoleGatewayCard(
            title = "🏪 نقطة بيع / بقالة",
            subtitle = "بوابة الشفافية الرقمية وكشف الحساب",
            accentColor = SemanticIncomeGreen,
            icon = Icons.Default.Storefront,
            tag = "splash_role_card_pos",
            onClick = { onSelectRole(AppUserRole.POS_MERCHANT) }
        )

        if (!errorMessage.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SemanticExpenseRed.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, SemanticExpenseRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = SemanticExpenseRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Returning user quick cloud recovery button
        OutlinedButton(
            onClick = onQuickCloudEmailRecover,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BrandCyanPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("splash_quick_google_recover_btn")
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = BrandCyanPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "حساب مسجل سابقاً؟ استرجاع الهوية السحابية عبر Google",
                color = Color(0xFFA5F3FC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SplashRoleGatewayCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B192C).copy(alpha = 0.92f)
        ),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SplashRoleEmailOnboardingCard(
    role: AppUserRole,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    displayNameInput: String,
    onDisplayNameChange: (String) -> Unit,
    agentCodeInput: String,
    onAgentCodeChange: (String) -> Unit,
    agentPinInput: String,
    onAgentPinChange: (String) -> Unit,
    posCodeInput: String,
    onPosCodeChange: (String) -> Unit,
    availableAgentCodes: List<AccessCodeEntry>,
    availablePosCodes: List<AccessCodeEntry>,
    errorMessage: String?,
    noticeMessage: String?,
    onBackToRoles: () -> Unit,
    onGoogleOneTapRecoverOrBind: () -> Unit,
    onCheckEmailCloudRecovery: () -> Unit,
    onSubmitRoleOnboarding: () -> Unit
) {
    val accentColor = when (role) {
        AppUserRole.NETWORK_OWNER -> BrandCyanPrimary
        AppUserRole.DISTRIBUTOR_AGENT -> Color(0xFF38BDF8)
        AppUserRole.POS_MERCHANT -> SemanticIncomeGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("splash_role_onboarding_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CyberDarkCardElevated),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (role) {
                            AppUserRole.NETWORK_OWNER -> "👑 إعداد حساب مالك الشبكة"
                            AppUserRole.DISTRIBUTOR_AGENT -> "🚚 دخول الوكيل / الموزع الميداني"
                            AppUserRole.POS_MERCHANT -> "🏪 دخول نقطة البيع / البقالة"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "يربط البريد الإلكتروني هويتك وصلاحياتك سحابياً لاسترجاعها تلقائياً على أي هاتف",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
                TextButton(
                    onClick = onBackToRoles,
                    modifier = Modifier.testTag("splash_back_to_roles_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("تغيير الدور", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = CyberBorder)

            // One-tap Google Sign-In button
            OutlinedButton(
                onClick = onGoogleOneTapRecoverOrBind,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("splash_role_google_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "اختيار حساب Google بنقرة (ربط أو استرجاع تلقائي)",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }

            // Email field + Cloud Recovery Lookup
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = onEmailChange,
                    label = { Text("البريد الإلكتروني للهوية السحابية") },
                    placeholder = { Text("user@gmail.com") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = accentColor)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("splash_email_input")
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCheckEmailCloudRecovery() }
                        .testTag("splash_check_email_recovery_btn")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "استرجاع تلقائي",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "فحص السحابة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            // Role-specific fields
            when (role) {
                AppUserRole.NETWORK_OWNER -> {
                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = onDisplayNameChange,
                        label = { Text("اسم الشبكة أو المالك (اختياري)") },
                        placeholder = { Text("مثال: شبكة المجد اللاسلكية") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("splash_owner_name_input")
                    )
                }

                AppUserRole.DISTRIBUTOR_AGENT -> {
                    OutlinedTextField(
                        value = agentCodeInput,
                        onValueChange = onAgentCodeChange,
                        label = { Text("كود دعوة الوكيل (AGT-XXXX)") },
                        placeholder = { Text("يُطلب أول مرة فقط ثم يُربط ببريدك") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = accentColor)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("splash_agent_code_input")
                    )

                    OutlinedTextField(
                        value = agentPinInput,
                        onValueChange = onAgentPinChange,
                        label = { Text("اسم الوكيل أو رمز PIN") },
                        placeholder = { Text("اختياري ما لم يحدد المالك رمز PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("splash_agent_pin_input")
                    )

                    if (availableAgentCodes.isNotEmpty()) {
                        Text(
                            text = "أكواد الوكلاء المسجلة على الجهاز (اختيار سريع):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableAgentCodes.take(4).forEach { entry ->
                                OutlinedButton(
                                    onClick = {
                                        onAgentCodeChange(entry.code)
                                        onAgentPinChange(entry.agentPin.ifBlank { entry.agentName })
                                    },
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                                ) {
                                    Text("${entry.agentName} (${entry.code})", color = accentColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                AppUserRole.POS_MERCHANT -> {
                    OutlinedTextField(
                        value = posCodeInput,
                        onValueChange = onPosCodeChange,
                        label = { Text("كود الشفافية الرقمية للبقالة (POS-XXXXXX)") },
                        placeholder = { Text("يُطلب أول مرة فقط ثم يُربط ببريدك") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = accentColor)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("splash_pos_code_input")
                    )

                    if (availablePosCodes.isNotEmpty()) {
                        Text(
                            text = "أكواد نقاط البيع المسجلة على الجهاز (اختيار سريع):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availablePosCodes.take(4).forEach { entry ->
                                OutlinedButton(
                                    onClick = { onPosCodeChange(entry.code) },
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                                ) {
                                    Text("${entry.partyName} (${entry.code})", color = accentColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (!noticeMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = noticeMessage,
                        color = accentColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SemanticExpenseRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SemanticExpenseRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = SemanticExpenseRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = onSubmitRoleOnboarding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color(0xFF03252E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("splash_submit_role_onboarding_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (role) {
                        AppUserRole.NETWORK_OWNER -> "حفظ الهوية والمتابعة لإعداد الميكروتك"
                        AppUserRole.DISTRIBUTOR_AGENT -> "ربط البريد والدخول بوضع الوكيل الميداني"
                        AppUserRole.POS_MERCHANT -> "ربط البريد وفتح بوابة نقطة البيع"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SplashPill(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F2636),
        border = BorderStroke(0.8.dp, BrandCyanPrimary.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFA5F3FC),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
