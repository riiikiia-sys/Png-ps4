package com.example

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ps4_icon_customizer.db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository by lazy {
        AppRepository(
            context = applicationContext,
            profileDao = database.profileDao(),
            transferLogDao = database.transferLogDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: AppViewModel = viewModel(
                    factory = AppViewModelFactory(repository)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        CosmicBackground,
                                        Color(0xFF030712),
                                        Color(0xFF001F54)
                                    )
                                )
                            )
                    ) {
                        Ps4IconCustomizerScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Ps4IconCustomizerScreen(viewModel: AppViewModel) {
    val ipAddress by viewModel.ipAddress.collectAsStateWithLifecycle()
    val port by viewModel.port.collectAsStateWithLifecycle()
    val titleId by viewModel.titleId.collectAsStateWithLifecycle()
    val remotePath by viewModel.remotePath.collectAsStateWithLifecycle()
    val isPathEdited by viewModel.isPathManuallyEdited.collectAsStateWithLifecycle()
    val selectedUri by viewModel.selectedUri.collectAsStateWithLifecycle()
    val fileMetadata by viewModel.fileMetadata.collectAsStateWithLifecycle()
    val transferState by viewModel.transferState.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
    val transferLogs by viewModel.transferLogs.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSaveDialog by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }
    
    // Scoped Storage compliant secure image-picker (supports Android 11 to 16)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onFileSelected(uri)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle Transfer Toast notifications of successes or failures
    LaunchedEffect(transferState) {
        when (transferState) {
            is TransferState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("icon0.png uploaded successfully to PS4!")
                }
            }
            is TransferState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error: ${(transferState as TransferState.Error).message}")
                }
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: PlayStation Aesthetic Premium Branding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▲",
                        color = Color(0xFF00FFCC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "◯",
                        color = Color(0xFFFF4B4B),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "✕",
                        color = Color(0xFF2E72F2),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "▢",
                        color = Color(0xFFFFB2E2),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "PS4 ICON CUSTOMIZER",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = TrophyGold
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Connect Over FTP to Replace Game & App Icons",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            // Connection Profile Configuration (Glassmorphism Styled Card)
            Text(
                text = "FTP CONNECTION PROFILE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PSBlueLight,
                    letterSpacing = 1.sp
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33FDD062), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile quick-selection row
                    if (savedProfiles.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Saved Profiles",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(savedProfiles) { profile ->
                                    val isSelected = ipAddress == profile.ipAddress &&
                                            port == profile.port.toString() &&
                                            titleId == profile.defaultTitleId &&
                                            remotePath == profile.manualRemotePath

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) PSBlueAccent.copy(alpha = 0.4f)
                                                else Color(0x1F2E72F2)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) TrophyGold else Color(0x33FFFFFF),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.selectProfile(profile) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = profile.name,
                                                color = if (isSelected) TrophyGoldLight else TextPrimary,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            IconButton(
                                                onClick = { viewModel.deleteProfile(profile) },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete Profile",
                                                    tint = ErrorRed.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                    }

                    // IP Address & Port (Editable row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { viewModel.onIpAddressChange(it) },
                            label = { Text("PS4 IP Address") },
                            placeholder = { Text("e.g. 192.168.1.100") },
                            modifier = Modifier
                                .weight(2f)
                                .testTag("ip_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrophyGold,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                cursorColor = TrophyGold,
                                focusedLabelColor = TrophyGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = port,
                            onValueChange = { viewModel.onPortChange(it) },
                            label = { Text("Port") },
                            placeholder = { Text("2121") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("port_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrophyGold,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                cursorColor = TrophyGold,
                                focusedLabelColor = TrophyGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Title ID Box & Dyn Path Editor
                    OutlinedTextField(
                        value = titleId,
                        onValueChange = { viewModel.onTitleIdChange(it) },
                        label = { Text("Title ID (Game ID)") },
                        placeholder = { Text("e.g. CUSA08252") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("title_id_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PSBlueAccent,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = PSBlueAccent,
                            focusedLabelColor = PSBlueAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Title ID Icon",
                                tint = PSBlueLight
                            )
                        }
                    )

                    // Manual Path Editor
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Remote Path",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPathEdited) TrophyGold else TextMuted
                                )
                            )
                            if (isPathEdited) {
                                Row(
                                    modifier = Modifier.clickable { viewModel.resetRemotePath() },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Path",
                                        tint = TrophyGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Reset Default",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TrophyGold)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = remotePath,
                            onValueChange = { viewModel.onRemotePathChange(it) },
                            placeholder = { Text("/user/appmeta/{TITLE_ID}/icon0.png") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_path_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isPathEdited) TrophyGold else PSBlueAccent,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                cursorColor = TrophyGold,
                                focusedLabelColor = TrophyGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        Text(
                            text = if (isPathEdited) "⚠️ Manual path override is ACTIVE" else "✓ Auto-syncs path to Title ID",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isPathEdited) TrophyGoldLight else TextMuted
                            )
                        )
                    }

                    // Save Profile Button
                    Button(
                        onClick = {
                            profileNameInput = "PS4 Profile (${ipAddress.takeLast(4)})"
                            showSaveDialog = true
                        },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x1F2E72F2),
                            contentColor = TextPrimary
                        ),
                        border = BorderStroke(1.dp, PSBlueAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Save Profile",
                                tint = PSBlueLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Save Current Profile", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Local File Select (Glassmorphism layout)
            Text(
                text = "PNG ICON SOURCE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PSBlueLight,
                    letterSpacing = 1.sp
                )
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clickable { filePickerLauncher.launch("image/png") }
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedUri == null) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0F2E72F2))
                                .border(
                                    width = 1.dp,
                                    color = Color(0x1AFFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PNG",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            )
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("image/png") },
                            modifier = Modifier.testTag("choose_png_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PSBlueAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Choose icon0.png File")
                        }

                        Text(
                            text = "Compatible with Scoped Storage. You can picker PNG files directly from Downloads or Pictures folders.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        )
                    } else {
                        // Image selected preview (Real coil loading)
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F1523))
                                .border(2.dp, TrophyGold, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = "Active image0 preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = fileMetadata?.second ?: "icon0.png",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TrophyGoldLight
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val fileSizeKb = (fileMetadata?.first ?: 0L) / 1024f
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f KB - Standard PNG", fileSizeKb),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("image/png") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Change File", fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = { viewModel.onFileSelected(null) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Selection", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Transfer / Active Actions Panel
            Spacer(modifier = Modifier.height(4.dp))

            // State display / progress bar
            AnimatedVisibility(
                visible = transferState !is TransferState.Idle,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F2E72F2)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (transferState) {
                            is TransferState.Uploading -> {
                                val progress = (transferState as TransferState.Uploading).progress
                                val percent = (progress * 100).toInt()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = TrophyGold,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Sending icon0.png...",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Text(
                                        text = "$percent%",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TrophyGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = TrophyGold,
                                    trackColor = Color(0x33FFFFFF)
                                )
                            }
                            is TransferState.Success -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success Icon",
                                        tint = SuccessGreen
                                    )
                                    Column {
                                        Text(
                                            text = "Transfer Succeeded",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                        )
                                        Text(
                                            text = "Restart PS4 dashboard if icon update doesn't reflect immediately.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                        )
                                    }
                                }
                            }
                            is TransferState.Error -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Error Icon",
                                        tint = ErrorRed
                                    )
                                    Column {
                                        Text(
                                            text = "Transfer Blocked / Failed",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ErrorRed
                                            )
                                        )
                                        Text(
                                            text = (transferState as TransferState.Error).message,
                                            style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            // Big Flash/Transfer Trigger Button
            Button(
                onClick = { viewModel.triggerFtpUpload() },
                enabled = selectedUri != null && transferState !is TransferState.Uploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("transfer_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TrophyGold,
                    contentColor = CosmicBackground,
                    disabledContainerColor = Color(0x1AFFFFFF),
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(16.dp),
                border = if (selectedUri != null) BorderStroke(1.dp, TrophyGoldLight) else null
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Flash Icon",
                        tint = if (selectedUri != null) CosmicBackground else TextMuted
                    )
                    Text(
                        text = if (transferState is TransferState.Uploading) "SENDING DATA..." else "FLASH / TRANSFER ICON0.PNG",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // Logs / Activity History Expandable Area
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT TRANSFER HISTORY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PSBlueLight,
                        letterSpacing = 1.sp
                    )
                )

                if (transferLogs.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearLogHistory() }) {
                        Text(
                            text = "Clear History",
                            style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1BFFFFFF), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (transferLogs.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Empty History",
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No transfers recorded yet.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        transferLogs.take(5).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0CFFFFFF), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (log.status == "SUCCESS") SuccessGreen.copy(alpha = 0.2f)
                                                    else ErrorRed.copy(alpha = 0.2f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = log.status,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (log.status == "SUCCESS") SuccessGreen else ErrorRed,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                        Text(
                                            text = log.titleId,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Path: ${log.destinationPath}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                                            Date(log.timestamp)
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextMuted.copy(alpha = 0.6f),
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                                if (log.errorMessage != null) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Log error details",
                                        tint = ErrorRed.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(log.errorMessage)
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Profile Custom Namer Alert Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Profile Connection") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Enter a name list label for this PS4 FTP connection configuration:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = profileNameInput,
                            onValueChange = { profileNameInput = it },
                            placeholder = { Text("e.g. My PS4 Pro") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrophyGold,
                                cursorColor = TrophyGold
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.saveCurrentProfile(profileNameInput)
                            showSaveDialog = false
                        }
                    ) {
                        Text("Save Profile", color = TrophyGold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                },
                containerColor = CosmicSurfaceCard,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Modern floating M3 Snackbar component
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
