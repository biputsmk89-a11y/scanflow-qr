package com.scanflow.qr.feature.generator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import com.scanflow.qr.ScanFlowApplication
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
import com.scanflow.qr.core.designsystem.ElectricBlueLight
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
    onNavigateToPreview: (Long) -> Unit,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFormOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        (context.applicationContext as? ScanFlowApplication)?.container?.appLockManager?.setTemporarilyBypassed(true)
        if (uri != null) {
            viewModel.importContactFromUri(context, uri)
        }
    }

    val whatsappContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        (context.applicationContext as? ScanFlowApplication)?.container?.appLockManager?.setTemporarilyBypassed(true)
        if (uri != null) {
            viewModel.importPhoneForWhatsapp(context, uri)
        }
    }

    val categories = remember {
        listOf(
            StitchQrCategoryItem(QrType.WEBSITE, "Website", "URL / Web Link", Icons.Default.Language, true),
            StitchQrCategoryItem(QrType.WHATSAPP, "WhatsApp", "Chat & Auto-Text", Icons.Default.Sms, true),
            StitchQrCategoryItem(QrType.TEXT, "Text", "Plain Text / Notes", Icons.Default.Notes, false),
            StitchQrCategoryItem(QrType.WIFI, "WiFi", "Network Credentials", Icons.Default.Wifi, true),
            StitchQrCategoryItem(QrType.BARCODE, "Barcode", "1D Code 128 / EAN / UPC", Icons.Default.ViewWeek, true),
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
                        text = if (isFormOpen) {
                            when (uiState.selectedType) {
                                QrType.WIFI -> "Create WiFi QR"
                                QrType.WHATSAPP -> "Create WhatsApp QR"
                                QrType.PAYMENT -> "ScanFlow QR"
                                QrType.CONTACT -> "Create Contact QR"
                                else -> uiState.selectedType.displayName
                            }
                        } else "ScanFlow QR",
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
                actions = {
                    if (onNavigateToSettings != null) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                when (uiState.selectedType) {
                    QrType.WIFI -> {
                        StitchWifiGeneratorView(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { isFormOpen = false },
                            onGenerate = {
                                viewModel.generateAndSaveQr { newId ->
                                    onNavigateToPreview(newId)
                                }
                            }
                        )
                    }
                    QrType.PAYMENT -> {
                        StitchPaymentGeneratorView(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { isFormOpen = false },
                            onGenerate = {
                                viewModel.generateAndSaveQr { newId ->
                                    onNavigateToPreview(newId)
                                }
                            }
                        )
                    }
                    QrType.CONTACT -> {
                        StitchContactGeneratorView(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBack = { isFormOpen = false },
                            onGenerate = {
                                viewModel.generateAndSaveQr { newId ->
                                    onNavigateToPreview(newId)
                                }
                            }
                        )
                    }
                    else -> {
                        // Form Generator for Other Selected Types
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
                                    OutlinedButton(
                                        onClick = {
                                            (context.applicationContext as? ScanFlowApplication)?.container?.appLockManager?.setTemporarilyBypassed(true)
                                            contactPickerLauncher.launch(null)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Ambil Langsung dari Kontak HP",
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricBlue,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(Dimens.Spacing16))

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
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.contactOrg,
                                        onValueChange = { viewModel.updateField { copy(contactOrg = it) } },
                                        label = "Organization / Company",
                                        placeholder = "ScanFlow Inc.",
                                        leadingIcon = Icons.Default.Business,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                    ScanFlowTextField(
                                        value = uiState.contactJobTitle,
                                        onValueChange = { viewModel.updateField { copy(contactJobTitle = it) } },
                                        label = "Job Title",
                                        placeholder = "Product Manager",
                                        leadingIcon = Icons.Default.Work,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                QrType.WHATSAPP -> {
                                    Text(
                                        text = "Kode Negara (Country Code)",
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
                                        listOf("+62 (ID)", "+1 (US)", "+60 (MY)", "+65 (SG)", "+44 (UK)", "+91 (IN)").forEach { codeLabel ->
                                            val code = codeLabel.substringBefore(" ")
                                            ScanFlowChip(
                                                text = codeLabel,
                                                selected = uiState.whatsappCountryCode == code,
                                                onClick = { viewModel.updateField { copy(whatsappCountryCode = code) } }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(Dimens.Spacing16))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Nomor WhatsApp",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                (context.applicationContext as? ScanFlowApplication)?.container?.appLockManager?.setTemporarilyBypassed(true)
                                                whatsappContactPickerLauncher.launch(null)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PersonAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = ElectricBlue
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Dari Kontak",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricBlue
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(Dimens.Spacing4))

                                    ScanFlowTextField(
                                        value = uiState.whatsappPhone,
                                        onValueChange = { viewModel.updateField { copy(whatsappPhone = it) } },
                                        label = "Nomor Telepon",
                                        placeholder = "Contoh: 08123456789 atau 8123456789",
                                        leadingIcon = Icons.Default.Call,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(Dimens.Spacing12))

                                    ScanFlowTextField(
                                        value = uiState.whatsappMessage,
                                        onValueChange = { viewModel.updateField { copy(whatsappMessage = it) } },
                                        label = "Draf Pesan Otomatis (Auto-Text)",
                                        placeholder = "Halo, saya tertarik dengan produk Anda...",
                                        singleLine = false,
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val cleanNum = viewModel.normalizeWhatsappNumber(uiState.whatsappCountryCode, uiState.whatsappPhone)
                                    if (cleanNum.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Tautan Langsung WhatsApp:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "https://wa.me/$cleanNum" + (if (uiState.whatsappMessage.isNotBlank()) "?text=..." else ""),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF25D366)
                                                )
                                            }
                                        }
                                    }
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
                                QrType.BARCODE -> {
                                    Text(
                                        text = "Barcode Symbology",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
                                    ) {
                                        listOf(
                                            "CODE_128" to "Code 128",
                                            "EAN_13" to "EAN-13",
                                            "UPC_A" to "UPC-A",
                                            "CODE_39" to "Code 39",
                                            "EAN_8" to "EAN-8"
                                        ).forEach { (format, label) ->
                                            ScanFlowChip(
                                                text = label,
                                                selected = uiState.barcodeFormat == format,
                                                onClick = { viewModel.updateField { copy(barcodeFormat = format) } }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(Dimens.Spacing16))
                                    ScanFlowTextField(
                                        value = uiState.barcodeContent,
                                        onValueChange = { viewModel.updateField { copy(barcodeContent = it) } },
                                        label = when (uiState.barcodeFormat) {
                                            "EAN_13" -> "EAN-13 Number (12-13 digit angka)"
                                            "EAN_8" -> "EAN-8 Number (7-8 digit angka)"
                                            "UPC_A" -> "UPC-A Number (11-12 digit angka)"
                                            "CODE_39" -> "Code 39 Text (A-Z, 0-9)"
                                            else -> "Barcode Value (ASCII Alphanumeric)"
                                        },
                                        placeholder = when (uiState.barcodeFormat) {
                                            "EAN_13" -> "Contoh: 8991234567890"
                                            "EAN_8" -> "Contoh: 96385074"
                                            "UPC_A" -> "Contoh: 012345678905"
                                            "CODE_39" -> "Contoh: ITEM-00123"
                                            else -> "Contoh: SCANFLOW-99"
                                        },
                                        leadingIcon = Icons.Default.ViewWeek,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                                    Text(
                                        text = when (uiState.barcodeFormat) {
                                            "EAN_13" -> "Standar ritel global barang konsumsi (12 digit angka + 1 digit checksum)."
                                            "EAN_8" -> "Versi ringkas EAN untuk kemasan produk berukuran kecil (7-8 digit angka)."
                                            "UPC_A" -> "Standar ritel Amerika Serikat & Kanada (11-12 digit angka)."
                                            "CODE_39" -> "Standar industri manufaktur. Mendukung huruf A-Z, angka 0-9, dan simbol (- . $ / + % spasi)."
                                            else -> "Format 1D densitas tinggi paling umum untuk logistik dan inventaris (mendukung teks ASCII)."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = if (uiState.selectedType == QrType.BARCODE) "Buat & Pratinjau Barcode" else "Generate & Customize QR",
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
}
}

/**
 * Stitch WiFi QR Generator View (Exact Google Stitch UI/UX)
 */
@Composable
private fun StitchWifiGeneratorView(
    uiState: CreateQrUiState,
    viewModel: CreateQrViewModel,
    onBack: () -> Unit,
    onGenerate: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.Spacing20),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Change Type Pill Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            ScanFlowSecondaryButton(
                text = "← Change Type",
                onClick = onBack
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Section (Exact Stitch)
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ElectricBlue.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "WiFi",
                tint = ElectricBlue,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "WiFi Network",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Instantly share your network connection without revealing your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Form Card (Elevation & Subtle Border)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Network Name (SSID)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Network Name (SSID)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ScanFlowTextField(
                        value = uiState.wifiSsid,
                        onValueChange = { viewModel.updateField { copy(wifiSsid = it) } },
                        label = "",
                        placeholder = "e.g., Home_Network_5G",
                        leadingIcon = Icons.Default.Router,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Password
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = uiState.wifiPassword,
                        onValueChange = { viewModel.updateField { copy(wifiPassword = it) } },
                        placeholder = { Text("Enter network password") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedContainerColor = ElectricBlue.copy(alpha = 0.04f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Security Type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Security Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "WPA" to "WPA/WPA2",
                            "WPA3" to "WPA3",
                            "WEP" to "WEP",
                            "nopass" to "None (Open)"
                        ).forEach { (sec, label) ->
                            ScanFlowChip(
                                text = label,
                                selected = uiState.wifiSecurity == sec,
                                onClick = { viewModel.updateField { copy(wifiSecurity = sec) } }
                            )
                        }
                    }
                }

                // Hidden Network
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Hidden Network",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "SSID doesn't broadcast",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.wifiHidden,
                            onCheckedChange = { viewModel.updateField { copy(wifiHidden = it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricBlue
                            )
                        )
                    }
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = Dimens.Spacing4)
                    )
                }

                // Generate Button (Pill shaped)
                Button(
                    onClick = onGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate WiFi QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * Stitch Payment QR Generator View (Exact Google Stitch UI/UX)
 */
@Composable
private fun StitchPaymentGeneratorView(
    uiState: CreateQrUiState,
    viewModel: CreateQrViewModel,
    onBack: () -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.Spacing20),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Change Type Pill Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            ScanFlowSecondaryButton(
                text = "← Change Type",
                onClick = onBack
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Section (Exact Stitch)
        Text(
            text = "Create Payment QR",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = ElectricBlue,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Generate a secure QR code for fast, contactless payments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Form Card (Surface Low Container)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Merchant Name
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Merchant Name",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ScanFlowTextField(
                        value = uiState.paymentPayeeName,
                        onValueChange = { viewModel.updateField { copy(paymentPayeeName = it) } },
                        label = "",
                        placeholder = "e.g. Acme Corp",
                        leadingIcon = Icons.Default.Storefront,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Amount
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ScanFlowTextField(
                        value = uiState.paymentAmount,
                        onValueChange = { viewModel.updateField { copy(paymentAmount = it) } },
                        label = "",
                        placeholder = "0.00",
                        leadingIcon = Icons.Default.AttachMoney,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Reference (Optional)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Reference (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ScanFlowTextField(
                        value = uiState.paymentReference,
                        onValueChange = { viewModel.updateField { copy(paymentReference = it) } },
                        label = "",
                        placeholder = "Invoice #12345",
                        leadingIcon = Icons.Default.ReceiptLong,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Payment URL / ID
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Payment URL / ID",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ScanFlowTextField(
                        value = uiState.paymentAddress,
                        onValueChange = { viewModel.updateField { copy(paymentAddress = it) } },
                        label = "",
                        placeholder = "https://pay.stripe.com/...",
                        leadingIcon = Icons.Default.Link,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = Dimens.Spacing4)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Generate Button (Pill shaped)
                Button(
                    onClick = onGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Payment QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Security Banner (Exact Stitch)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CyanAccent.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Always verify payment details before completing a transaction. Generated codes are encrypted for security.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
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

/**
 * Stitch Contact QR Generator View (Exact Google Stitch UI/UX)
 * Reference: Project ScanFlow QR Ecosystem - Screen "Contact QR Generator"
 */
@Composable
private fun StitchContactGeneratorView(
    uiState: CreateQrUiState,
    viewModel: CreateQrViewModel,
    onBack: () -> Unit,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.Spacing20),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Change Type Pill Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            ScanFlowSecondaryButton(
                text = "← Change Type",
                onClick = onBack
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Section (Exact Google Stitch Avatar + Description)
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    spotColor = ElectricBlue.copy(alpha = 0.4f),
                    ambientColor = ElectricBlue.copy(alpha = 0.2f)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ElectricBlueLight, ElectricBlue)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Contact Avatar",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Create a digital business card. Share your contact info instantly via QR code.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Form Section (Card Container with 24dp radius and subtle border)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Full Name (Required)
                StitchContactField(
                    value = uiState.contactName,
                    onValueChange = { viewModel.updateField { copy(contactName = it) } },
                    placeholder = "Full Name",
                    leadingIcon = Icons.Default.Person,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                // 2. Phone Number
                StitchContactField(
                    value = uiState.contactPhone,
                    onValueChange = { viewModel.updateField { copy(contactPhone = it) } },
                    placeholder = "Phone Number",
                    leadingIcon = Icons.Default.Call,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                // 3. Email Address
                StitchContactField(
                    value = uiState.contactEmail,
                    onValueChange = { viewModel.updateField { copy(contactEmail = it) } },
                    placeholder = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                // 4. Organization / Company
                StitchContactField(
                    value = uiState.contactOrg,
                    onValueChange = { viewModel.updateField { copy(contactOrg = it) } },
                    placeholder = "Organization / Company",
                    leadingIcon = Icons.Default.Business,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                // 5. Job Title
                StitchContactField(
                    value = uiState.contactJobTitle,
                    onValueChange = { viewModel.updateField { copy(contactJobTitle = it) } },
                    placeholder = "Job Title",
                    leadingIcon = Icons.Default.Work,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                // 6. Website URL
                StitchContactField(
                    value = uiState.contactWebsite,
                    onValueChange = { viewModel.updateField { copy(contactWebsite = it) } },
                    placeholder = "Website URL",
                    leadingIcon = Icons.Default.Language,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                // 7. Full Address
                StitchContactField(
                    value = uiState.contactAddress,
                    onValueChange = { viewModel.updateField { copy(contactAddress = it) } },
                    placeholder = "Full Address",
                    leadingIcon = Icons.Default.LocationOn,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = Dimens.Spacing4)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary CTA (Generate Contact QR)
                Button(
                    onClick = onGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = ElectricBlue.copy(alpha = 0.45f)
                        ),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Contact QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * Clean filled text field matching Google Stitch design specification:
 * - Rounded 14dp corners
 * - primary/5 tint background
 * - Leading icon in outline variant color
 * - Active focus border ElectricBlue
 */
@Composable
private fun StitchContactField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = placeholder,
                tint = MaterialTheme.colorScheme.outline
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = ElectricBlue.copy(alpha = 0.07f),
            unfocusedContainerColor = ElectricBlue.copy(alpha = 0.04f),
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.fillMaxWidth()
    )
}

