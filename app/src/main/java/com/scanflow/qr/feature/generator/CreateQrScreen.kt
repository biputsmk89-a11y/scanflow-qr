package com.scanflow.qr.feature.generator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.ScanFlowChip
import com.scanflow.qr.core.designsystem.ScanFlowPrimaryButton
import com.scanflow.qr.core.designsystem.ScanFlowSecondaryButton
import com.scanflow.qr.core.designsystem.ScanFlowTextField
import com.scanflow.qr.core.designsystem.SectionHeader
import com.scanflow.qr.domain.model.QrType

data class StitchQrCategoryItem(
    val type: QrType,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isPrimaryColor: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQrScreen(
    viewModel: CreateQrViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFormOpen by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            StitchQrCategoryItem(QrType.WEBSITE, "Website", "URL / Web Link", Icons.Default.Language, true),
            StitchQrCategoryItem(QrType.TEXT, "Text", "Plain Text / Notes", Icons.Default.Notes, false),
            StitchQrCategoryItem(QrType.WIFI, "WiFi", "Network Credentials", Icons.Default.Wifi, true),
            StitchQrCategoryItem(QrType.CONTACT, "Contact", "vCard Profile", Icons.Default.ContactPage, false),
            StitchQrCategoryItem(QrType.EMAIL, "Email", "Send Mail", Icons.Default.Email, true),
            StitchQrCategoryItem(QrType.PHONE, "Phone", "Direct Dial", Icons.Default.Call, false),
            StitchQrCategoryItem(QrType.SMS, "SMS", "Text Message", Icons.Default.Sms, true),
            StitchQrCategoryItem(QrType.LOCATION, "Location", "Geo Coordinates", Icons.Default.LocationOn, false),
            StitchQrCategoryItem(QrType.PAYMENT, "Payment", "Crypto / UPI / PayPal", Icons.Default.Payments, true),
            StitchQrCategoryItem(QrType.SOCIAL, "Social Media", "Instagram, X, etc.", Icons.Default.Share, false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isFormOpen) uiState.selectedType.displayName else "ScanFlow QR",
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isFormOpen) {
                                isFormOpen = false
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AnimatedContent(
            targetState = isFormOpen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "CreatorFormTransition",
            modifier = Modifier.padding(padding)
        ) { formActive ->
            if (!formActive) {
                // Stitch QR Creator Menu (Bento Grid of Content Types)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.Spacing20)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Create QR Code",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a content type to generate a new QR code.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Dimens.Spacing24))

                    Text(
                        text = "Content Types",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing12))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(categories) { cat ->
                            StitchBentoTypeCard(
                                item = cat,
                                onClick = {
                                    viewModel.selectType(cat.type)
                                    isFormOpen = true
                                }
                            )
                        }
                    }
                }
            } else {
                // Form Generator for Selected Type
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.Spacing20)
                ) {
                    // Back to Categories Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScanFlowSecondaryButton(
                            text = "← Change Type",
                            onClick = { isFormOpen = false }
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = uiState.selectedType.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Spacing16))

                    // Dynamic Form Card
                    SectionHeader(title = "Enter Information")
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))

                    ScanFlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(Dimens.Spacing20)) {
                            when (uiState.selectedType) {
                                QrType.WEBSITE -> {
                                    ScanFlowTextField(
                                        value = uiState.textOrUrl,
                                        onValueChange = { viewModel.updateField { copy(textOrUrl = it) } },
                                        label = "Website URL",
                                        placeholder = "https://example.com",
                                        leadingIcon = Icons.Default.Language,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.TEXT -> {
                                    ScanFlowTextField(
                                        value = uiState.textOrUrl,
                                        onValueChange = { viewModel.updateField { copy(textOrUrl = it) } },
                                        label = "Plain Text",
                                        placeholder = "Enter any message, notes, or raw data",
                                        leadingIcon = Icons.Default.Notes,
                                        singleLine = false,
                                        maxLines = 5,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.WIFI -> {
                                    ScanFlowTextField(
                                        value = uiState.wifiSsid,
                                        onValueChange = { viewModel.updateField { copy(wifiSsid = it) } },
                                        label = "Network Name (SSID)",
                                        placeholder = "Home_WiFi",
                                        leadingIcon = Icons.Default.Wifi,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.wifiPassword,
                                        onValueChange = { viewModel.updateField { copy(wifiPassword = it) } },
                                        label = "Wi-Fi Password",
                                        placeholder = "Leave empty if open network",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    Text(
                                        text = "Security Protocol",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
                                    ) {
                                        listOf("WPA" to "WPA/WPA2", "WEP" to "WEP", "nopass" to "Open").forEach { (sec, label) ->
                                            ScanFlowChip(
                                                text = label,
                                                selected = uiState.wifiSecurity == sec,
                                                onClick = { viewModel.updateField { copy(wifiSecurity = sec) } }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Hidden Network (SSID Tersembunyi)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Switch(
                                            checked = uiState.wifiHidden,
                                            onCheckedChange = { viewModel.updateField { copy(wifiHidden = it) } }
                                        )
                                    }
                                }
                                QrType.CONTACT -> {
                                    ScanFlowTextField(
                                        value = uiState.contactName,
                                        onValueChange = { viewModel.updateField { copy(contactName = it) } },
                                        label = "Full Name",
                                        placeholder = "John Doe",
                                        leadingIcon = Icons.Default.Person,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.contactPhone,
                                        onValueChange = { viewModel.updateField { copy(contactPhone = it) } },
                                        label = "Phone Number",
                                        placeholder = "+62 812 3456 7890",
                                        leadingIcon = Icons.Default.Call,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.contactEmail,
                                        onValueChange = { viewModel.updateField { copy(contactEmail = it) } },
                                        label = "Email Address",
                                        placeholder = "john@example.com",
                                        leadingIcon = Icons.Default.Email,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.EMAIL -> {
                                    ScanFlowTextField(
                                        value = uiState.emailTo,
                                        onValueChange = { viewModel.updateField { copy(emailTo = it) } },
                                        label = "Recipient Email",
                                        placeholder = "contact@scanflow.io",
                                        leadingIcon = Icons.Default.Email,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.emailSubject,
                                        onValueChange = { viewModel.updateField { copy(emailSubject = it) } },
                                        label = "Subject",
                                        placeholder = "Hello from ScanFlow",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.emailBody,
                                        onValueChange = { viewModel.updateField { copy(emailBody = it) } },
                                        label = "Message Body",
                                        placeholder = "Your message here...",
                                        singleLine = false,
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.PHONE -> {
                                    ScanFlowTextField(
                                        value = uiState.phoneNumber,
                                        onValueChange = { viewModel.updateField { copy(phoneNumber = it) } },
                                        label = "Phone Number",
                                        placeholder = "+62 812 3456 7890",
                                        leadingIcon = Icons.Default.Call,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.SMS -> {
                                    ScanFlowTextField(
                                        value = uiState.smsPhone,
                                        onValueChange = { viewModel.updateField { copy(smsPhone = it) } },
                                        label = "Phone Number",
                                        placeholder = "+62 812 3456 7890",
                                        leadingIcon = Icons.Default.Sms,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.smsMessage,
                                        onValueChange = { viewModel.updateField { copy(smsMessage = it) } },
                                        label = "Predefined Message",
                                        placeholder = "Hello, I am interested in...",
                                        singleLine = false,
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.LOCATION -> {
                                    ScanFlowTextField(
                                        value = uiState.locationLat,
                                        onValueChange = { viewModel.updateField { copy(locationLat = it) } },
                                        label = "Latitude",
                                        placeholder = "-6.2088",
                                        leadingIcon = Icons.Default.LocationOn,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.locationLng,
                                        onValueChange = { viewModel.updateField { copy(locationLng = it) } },
                                        label = "Longitude",
                                        placeholder = "106.8456",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.PAYMENT -> {
                                    Text(
                                        text = "Payment Method",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
                                    ) {
                                        listOf("UPI", "Bitcoin", "Ethereum", "PayPal").forEach { method ->
                                            ScanFlowChip(
                                                text = method,
                                                selected = uiState.paymentType == method,
                                                onClick = { viewModel.updateField { copy(paymentType = method) } }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.paymentAddress,
                                        onValueChange = { viewModel.updateField { copy(paymentAddress = it) } },
                                        label = "${uiState.paymentType} Address / ID",
                                        placeholder = "Wallet or payment identifier",
                                        leadingIcon = Icons.Default.Payments,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.SOCIAL -> {
                                    Text(
                                        text = "Social Platform",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
                                    ) {
                                        listOf("Instagram", "Twitter / X", "TikTok", "LinkedIn", "YouTube").forEach { platform ->
                                            ScanFlowChip(
                                                text = platform,
                                                selected = uiState.socialPlatform == platform,
                                                onClick = { viewModel.updateField { copy(socialPlatform = platform) } }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.socialUsername,
                                        onValueChange = { viewModel.updateField { copy(socialUsername = it) } },
                                        label = "${uiState.socialPlatform} Username",
                                        placeholder = "username without @",
                                        leadingIcon = Icons.Default.Share,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                else -> {}
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(Dimens.Spacing8))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = Dimens.Spacing8)
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.Spacing24))

                    // Generate Button
                    ScanFlowPrimaryButton(
                        text = "Generate & Customize QR",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            viewModel.generateAndSaveQr { newId ->
                                onNavigateToPreview(newId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        gradient = true
                    )

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

/**
 * Stitch Bento Grid Card for Content Types
 */
@Composable
private fun StitchBentoTypeCard(
    item: StitchQrCategoryItem,
    onClick: () -> Unit
) {
    val iconColor = if (item.isPrimaryColor) ElectricBlue else CyanAccent
    val bgTint = if (item.isPrimaryColor) ElectricBlue.copy(alpha = 0.12f) else CyanAccent.copy(alpha = 0.15f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
