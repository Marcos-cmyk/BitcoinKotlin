package com.marcos.bitcoinkotlin.beginner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.marcos.bitcoin.BitcoinV1
import com.marcos.bitcoin.models.BitcoinWallet
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Generate Wallet Activity
 */
class GenerateWalletActivity : ComponentActivity() {
    
    // Mnemonic length options
    private val mnemonicLengthOptions = listOf(
        128 to "12 words (128-bit)",
        160 to "15 words (160-bit)",
        192 to "18 words (192-bit)",
        224 to "21 words (224-bit)",
        256 to "24 words (256-bit)"
    )
    
    // Mnemonic language options
    private val mnemonicLanguageOptions = listOf(
        "english" to "English",
        "chinese_simplified" to "中文 (Simplified Chinese)",
        "chinese_traditional" to "中文 (Traditional Chinese)",
        "korean" to "한국어 (Korean)",
        "japanese" to "日本語 (Japanese)",
        "french" to "Français (French)",
        "italian" to "Italiano (Italian)",
        "spanish" to "Español (Spanish)",
        "czech" to "Čeština (Czech)",
        "portuguese" to "Português (Portuguese)"
    )
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("GenerateWallet", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GenerateWalletScreen(
                        bitcoin = bitcoin,
                        mnemonicLengthOptions = mnemonicLengthOptions,
                        mnemonicLanguageOptions = mnemonicLanguageOptions
                    )
                }
            }
        }
    }
}

@Composable
fun GenerateWalletScreen(
    bitcoin: BitcoinV1,
    mnemonicLengthOptions: List<Pair<Int, String>>,
    mnemonicLanguageOptions: List<Pair<String, String>>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var selectedMnemonicLengthIndex by remember { mutableStateOf(0) }
    var selectedLanguageIndex by remember { mutableStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }
    var wallet by remember { mutableStateOf<BitcoinWallet?>(null) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Auto-dismiss snackbar
    LaunchedEffect(showToastMessage) {
        showToastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            delay(2000)
            showToastMessage = null
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Generate Wallet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Network Selection Section
            SectionHeader("Network Type")
            NetworkSelector(
                isTestnet = isTestnet,
                onNetworkChanged = { isTestnet = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mnemonic Length Selection
            SectionHeader("Mnemonic Length")
            MnemonicLengthSelector(
                options = mnemonicLengthOptions,
                selectedIndex = selectedMnemonicLengthIndex,
                onSelectionChanged = { selectedMnemonicLengthIndex = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mnemonic Language Selection
            SectionHeader("Mnemonic Language")
            MnemonicLanguageSelector(
                options = mnemonicLanguageOptions,
                selectedIndex = selectedLanguageIndex,
                onSelectionChanged = { selectedLanguageIndex = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Generate Button
            Button(
                onClick = {
                    if (!isGenerating) {
                        scope.launch {
                            generateWallet(
                                bitcoin = bitcoin,
                                mnemonicLength = mnemonicLengthOptions[selectedMnemonicLengthIndex].first,
                                isTestnet = isTestnet,
                                language = mnemonicLanguageOptions[selectedLanguageIndex].first,
                                onGeneratingChanged = { isGenerating = it },
                                onWalletGenerated = { wallet = it },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isGenerating) "Generating..." else "Generate Wallet")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Wallet Information Section
            wallet?.let { currentWallet ->
                WalletInfoSection(
                    wallet = currentWallet,
                    onCopyAll = {
                        copyAllWalletData(context, currentWallet, isTestnet, mnemonicLengthOptions[selectedMnemonicLengthIndex].first)
                        showToastMessage = "All wallet data copied"
                    },
                    onCopyMnemonic = {
                        copyToClipboard(context, currentWallet.mnemonic ?: "", "Mnemonic")
                        showToastMessage = "Mnemonic copied"
                    },
                    onCopyPrivateKey = {
                        copyToClipboard(context, currentWallet.privateKey, "Private Key")
                        showToastMessage = "Private key copied"
                    },
                    onCopyPublicKey = {
                        copyToClipboard(context, currentWallet.publicKey, "Public Key")
                        showToastMessage = "Public key copied"
                    },
                    onCopyAddress = { address, type ->
                        copyToClipboard(context, address, "$type Address")
                        showToastMessage = "$type address copied"
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun NetworkSelector(
    isTestnet: Boolean,
    onNetworkChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Network:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !isTestnet,
                onClick = { onNetworkChanged(false) },
                label = { Text("Mainnet") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = isTestnet,
                onClick = { onNetworkChanged(true) },
                label = { Text("Testnet") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemonicLengthSelector(
    options: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Length:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = options[selectedIndex].second,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            onSelectionChanged(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemonicLanguageSelector(
    options: List<Pair<String, String>>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Language:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = options[selectedIndex].second,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option.second) },
                        onClick = {
                            onSelectionChanged(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WalletInfoSection(
    wallet: BitcoinWallet,
    onCopyAll: () -> Unit,
    onCopyMnemonic: () -> Unit,
    onCopyPrivateKey: () -> Unit,
    onCopyPublicKey: () -> Unit,
    onCopyAddress: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Copy All button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallet Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onCopyAll) {
                    Text("Copy All JSON")
                }
            }
            
            HorizontalDivider()
            
            // Mnemonic
            InfoRow(
                label = "Mnemonic:",
                value = wallet.mnemonic ?: "-",
                onCopy = onCopyMnemonic,
                valueColor = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider()
            
            // Private Key
            InfoRow(
                label = "Private Key (Hex):",
                value = wallet.privateKey,
                onCopy = onCopyPrivateKey
            )
            
            HorizontalDivider()
            
            // Public Key
            InfoRow(
                label = "Public Key (Hex):",
                value = wallet.publicKey,
                onCopy = onCopyPublicKey
            )
            
            HorizontalDivider()
            
            // Addresses
            AddressRow(
                label = "Legacy Address (P2PKH):",
                address = wallet.addresses.legacy,
                onCopy = { onCopyAddress(wallet.addresses.legacy, "Legacy") }
            )
            
            HorizontalDivider()
            
            AddressRow(
                label = "Segwit Address (P2WPKH):",
                address = wallet.addresses.segwit,
                onCopy = { onCopyAddress(wallet.addresses.segwit, "Segwit") }
            )
            
            HorizontalDivider()
            
            AddressRow(
                label = "Taproot Address (P2TR):",
                address = wallet.addresses.taproot,
                onCopy = { onCopyAddress(wallet.addresses.taproot, "Taproot") }
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AddressRow(
    label: String,
    address: String,
    onCopy: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = address,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

suspend fun generateWallet(
    bitcoin: BitcoinV1,
    mnemonicLength: Int,
    isTestnet: Boolean,
    language: String,
    onGeneratingChanged: (Boolean) -> Unit,
    onWalletGenerated: (BitcoinWallet) -> Unit,
    onError: (String) -> Unit
) {
    onGeneratingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            // Wait for initialization
            delay(500)
        }
        
        // Create account using async/await
        val result: Triple<Boolean, BitcoinWallet?, String?> = bitcoin.createAccount(
            mnemonicLength = mnemonicLength,
            isTestnet = isTestnet,
            language = language
        )
        val success = result.first
        val wallet = result.second
        val error = result.third
        
        onGeneratingChanged(false)
        
        if (success && wallet != null) {
            onWalletGenerated(wallet)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onGeneratingChanged(false)
        onError("Error: ${e.message}")
    }
}

fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

fun copyAllWalletData(
    context: Context,
    wallet: BitcoinWallet,
    isTestnet: Boolean,
    mnemonicLength: Int
) {
    val walletData = JSONObject().apply {
        put("network", if (isTestnet) "testnet" else "mainnet")
        put("mnemonicLength", mnemonicLength)
        put("mnemonic", wallet.mnemonic ?: "")
        put("privateKey", wallet.privateKey)
        put("publicKey", wallet.publicKey)
        put("addresses", JSONObject().apply {
            put("legacy", wallet.addresses.legacy)
            put("segwit", wallet.addresses.segwit)
            put("taproot", wallet.addresses.taproot)
        })
    }
    
    val jsonString = walletData.toString(2) // Pretty print with 2 spaces indentation
    copyToClipboard(context, jsonString, "Wallet Data")
}
