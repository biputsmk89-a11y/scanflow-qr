package com.scanflow.qr.feature.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.ScanFlowChip
import com.scanflow.qr.core.designsystem.ScanFlowPrimaryButton
import com.scanflow.qr.core.designsystem.ScanFlowTextField
import com.scanflow.qr.core.designsystem.SectionHeader
import com.scanflow.qr.domain.model.QrType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQrScreen(
    viewModel: CreateQrViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val availableTypes = listOf(
        QrType.WEBSITE,
        QrType.TEXT,
        QrType.WIFI,
        QrType.CONTACT,
        QrType.EMAIL,
        QrType.PHONE,
        QrType.SMS,
        QrType.LOCATION,
        QrType.CALENDAR,
        QrType.PAYMENT,
        QrType.SOCIAL
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create QR Code", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Spacing20)
        ) {
            Spacer(modifier = Modifier.height(Dimens.Spacing8))

            // Type Selection Carousel
            SectionHeader(title = "Select QR Type")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
            ) {
                availableTypes.forEach { type ->
                    ScanFlowChip(
                        text = type.displayName.substringBefore(" "),
                        selected = uiState.selectedType == type,
                        onClick = { viewModel.selectType(type) },
                        icon = when (type) {
                            QrType.WEBSITE -> Icons.Default.Language
                            QrType.TEXT -> Icons.Default.TextFields
                            QrType.WIFI -> Icons.Default.Wifi
                            QrType.CONTACT -> Icons.Default.Person
                            QrType.EMAIL -> Icons.Default.Email
                            QrType.PHONE -> Icons.Default.Call
                            QrType.SMS -> Icons.Default.Sms
                            QrType.LOCATION -> Icons.Default.LocationOn
                            QrType.CALENDAR -> Icons.Default.Event
                            QrType.PAYMENT -> Icons.Default.Payment
                            QrType.SOCIAL -> Icons.Default.Share
                            else -> null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

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
                                placeholder = "Enter any message, notes, or data",
                                leadingIcon = Icons.Default.TextFields,
                                singleLine = false,
                                maxLines = 4,
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
                                listOf("WPA" to "WPA/WPA2", "WEP" to "WEP", "nopass" to "Open (None)").forEach { (sec, label) ->
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
                                placeholder = "+1 234 567 8900",
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
                                label = "Company / Organization",
                                placeholder = "Acme Corp",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrType.EMAIL -> {
                            ScanFlowTextField(
                                value = uiState.emailTo,
                                onValueChange = { viewModel.updateField { copy(emailTo = it) } },
                                label = "Recipient Email",
                                placeholder = "support@company.com",
                                leadingIcon = Icons.Default.Email,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.Spacing12))
                            ScanFlowTextField(
                                value = uiState.emailSubject,
                                onValueChange = { viewModel.updateField { copy(emailSubject = it) } },
                                label = "Email Subject",
                                placeholder = "Feedback or Inquiry",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.Spacing12))
                            ScanFlowTextField(
                                value = uiState.emailBody,
                                onValueChange = { viewModel.updateField { copy(emailBody = it) } },
                                label = "Message Body",
                                placeholder = "Write your message here...",
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrType.PHONE -> {
                            ScanFlowTextField(
                                value = uiState.phoneNumber,
                                onValueChange = { viewModel.updateField { copy(phoneNumber = it) } },
                                label = "Phone Number",
                                placeholder = "+1 234 567 890",
                                leadingIcon = Icons.Default.Call,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrType.SMS -> {
                            ScanFlowTextField(
                                value = uiState.smsPhone,
                                onValueChange = { viewModel.updateField { copy(smsPhone = it) } },
                                label = "Recipient Phone Number",
                                placeholder = "+1 234 567 890",
                                leadingIcon = Icons.Default.Sms,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.Spacing12))
                            ScanFlowTextField(
                                value = uiState.smsMessage,
                                onValueChange = { viewModel.updateField { copy(smsMessage = it) } },
                                label = "SMS Message",
                                placeholder = "Type message text...",
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
                                leadingIcon = Icons.Default.LocationOn,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrType.CALENDAR -> {
                            ScanFlowTextField(
                                value = uiState.calendarTitle,
                                onValueChange = { viewModel.updateField { copy(calendarTitle = it) } },
                                label = "Event Title",
                                placeholder = "Product Launch Meeting",
                                leadingIcon = Icons.Default.Event,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.Spacing12))
                            ScanFlowTextField(
                                value = uiState.calendarDate,
                                onValueChange = { viewModel.updateField { copy(calendarDate = it) } },
                                label = "Event Date / Time (Optional)",
                                placeholder = "2026-10-15 or 20261015T090000",
                                leadingIcon = Icons.Default.Event,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.Spacing12))
                            ScanFlowTextField(
                                value = uiState.calendarLocation,
                                onValueChange = { viewModel.updateField { copy(calendarLocation = it) } },
                                label = "Event Location",
                                placeholder = "Building 4, Room 201",
                                leadingIcon = Icons.Default.LocationOn,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(Dimens.Spacing12))
                            ScanFlowTextField(
                                value = uiState.calendarDescription,
                                onValueChange = { viewModel.updateField { copy(calendarDescription = it) } },
                                label = "Description (Optional)",
                                placeholder = "Agenda, notes, or details...",
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        QrType.PAYMENT -> {
                            Text(
                                text = "Payment Method / Currency",
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
                                label = when (uiState.paymentType) {
                                    "UPI" -> "VPA / UPI ID (e.g. name@upi)"
                                    "Bitcoin" -> "Bitcoin Wallet Address"
                                    "Ethereum" -> "Ethereum Wallet Address"
                                    else -> "PayPal Username / Handle"
                                },
                                placeholder = when (uiState.paymentType) {
                                    "UPI" -> "merchant@oksbi"
                                    "Bitcoin" -> "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"
                                    "Ethereum" -> "0x71C...8976F"
                                    else -> "your_paypal_tag"
                                },
                                leadingIcon = Icons.Default.Payment,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.paymentType == "UPI") {
                                Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                ScanFlowTextField(
                                    value = uiState.paymentPayeeName,
                                    onValueChange = { viewModel.updateField { copy(paymentPayeeName = it) } },
                                    label = "Payee Name (Optional)",
                                    placeholder = "Merchant or Business Name",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(Dimens.Spacing12))
                                ScanFlowTextField(
                                    value = uiState.paymentAmount,
                                    onValueChange = { viewModel.updateField { copy(paymentAmount = it) } },
                                    label = "Amount (Optional)",
                                    placeholder = "100.00",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        QrType.SOCIAL -> {
                            Text(
                                text = "Select Platform",
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
                                listOf("Instagram", "Twitter / X", "TikTok", "LinkedIn", "GitHub", "YouTube").forEach { platform ->
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

            Spacer(modifier = Modifier.height(Dimens.Spacing32))
        }
    }
}
