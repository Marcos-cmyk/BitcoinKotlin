package com.marcos.bitcoinkotlin.beginner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.marcos.bitcoin.BitcoinV1
import com.marcos.bitcoin.models.BitcoinWallet
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Import Wallet Activity
 */
class ImportWalletActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("ImportWallet", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImportWalletScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun ImportWalletScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var isImportingMnemonic by remember { mutableStateOf(true) }
    var mnemonicText by remember { mutableStateOf("") }
    var privateKeyText by remember { mutableStateOf("") }
    var isPrivateKeyVisible by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
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
                text = "Import Wallet",
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
            
            // Import Type Selection
            SectionHeader("Import Method")
            ImportTypeSelector(
                isMnemonic = isImportingMnemonic,
                onImportTypeChanged = { isImportingMnemonic = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mnemonic Input Section
            if (isImportingMnemonic) {
                SectionHeader("Mnemonic")
                MnemonicInputField(
                    value = mnemonicText,
                    onValueChange = { mnemonicText = it }
                )
            }
            
            // Private Key Input Section
            if (!isImportingMnemonic) {
                SectionHeader("Private Key")
                PrivateKeyInputField(
                    value = privateKeyText,
                    onValueChange = { privateKeyText = it },
                    isVisible = isPrivateKeyVisible,
                    onVisibilityToggle = { isPrivateKeyVisible = !isPrivateKeyVisible }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Import Button
            Button(
                onClick = {
                    if (!isImporting) {
                        scope.launch {
                            if (isImportingMnemonic) {
                                // Validate mnemonic
                                val trimmedMnemonic = mnemonicText.trim()
                                if (trimmedMnemonic.isEmpty()) {
                                    showToastMessage = "Please enter mnemonic"
                                    return@launch
                                }
                                
                                val words = trimmedMnemonic.split(Regex("\\s+")).filter { it.isNotEmpty() }
                                if (words.size != 12 && words.size != 24) {
                                    showToastMessage = "Mnemonic must be 12 or 24 words"
                                    return@launch
                                }
                                
                                importWalletFromMnemonic(
                                    bitcoin = bitcoin,
                                    mnemonic = trimmedMnemonic,
                                    isTestnet = isTestnet,
                                    onImportingChanged = { isImporting = it },
                                    onWalletImported = { wallet = it },
                                    onError = { showToastMessage = it }
                                )
                            } else {
                                // Validate private key
                                val trimmedPrivateKey = privateKeyText.trim()
                                if (trimmedPrivateKey.isEmpty()) {
                                    showToastMessage = "Please enter private key"
                                    return@launch
                                }
                                
                                if (trimmedPrivateKey.length != 64 || !trimmedPrivateKey.matches(Regex("[0-9a-fA-F]+"))) {
                                    showToastMessage = "Private key must be 64 hexadecimal characters"
                                    return@launch
                                }
                                
                                importWalletFromPrivateKey(
                                    bitcoin = bitcoin,
                                    privateKey = trimmedPrivateKey,
                                    isTestnet = isTestnet,
                                    onImportingChanged = { isImporting = it },
                                    onWalletImported = { wallet = it },
                                    onError = { showToastMessage = it }
                                )
                            }
                        }
                    }
                },
                enabled = !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isImporting) "Importing..." else "Import Wallet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Wallet Information Section
            wallet?.let { currentWallet ->
                ImportWalletInfoSection(
                    wallet = currentWallet,
                    isTestnet = isTestnet,
                    onCopyAll = {
                        copyAllWalletDataForImport(context, currentWallet, isTestnet)
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
fun ImportTypeSelector(
    isMnemonic: Boolean,
    onImportTypeChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = isMnemonic,
            onClick = { onImportTypeChanged(true) },
            label = { Text("Mnemonic") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = !isMnemonic,
            onClick = { onImportTypeChanged(false) },
            label = { Text("Private Key") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MnemonicInputField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    text = "Enter 12 or 24 mnemonic words, separated by spaces\nExample: abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            },
            maxLines = 5,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
fun PrivateKeyInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Enter hexadecimal private key (64 characters)",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (isVisible) "Hide" else "Show"
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            singleLine = true
        )
    }
}

suspend fun importWalletFromMnemonic(
    bitcoin: BitcoinV1,
    mnemonic: String,
    isTestnet: Boolean,
    onImportingChanged: (Boolean) -> Unit,
    onWalletImported: (BitcoinWallet) -> Unit,
    onError: (String) -> Unit
) {
    onImportingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, BitcoinWallet?, String?> = bitcoin.importAccountFromMnemonic(
            mnemonic = mnemonic,
            isTestnet = isTestnet,
            language = null
        )
        val success = result.first
        val wallet = result.second
        val error = result.third
        
        onImportingChanged(false)
        
        if (success && wallet != null) {
            onWalletImported(wallet)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onImportingChanged(false)
        onError("Error: ${e.message}")
    }
}

suspend fun importWalletFromPrivateKey(
    bitcoin: BitcoinV1,
    privateKey: String,
    isTestnet: Boolean,
    onImportingChanged: (Boolean) -> Unit,
    onWalletImported: (BitcoinWallet) -> Unit,
    onError: (String) -> Unit
) {
    onImportingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, BitcoinWallet?, String?> = bitcoin.importAccountFromPrivateKey(
            privateKey = privateKey,
            isTestnet = isTestnet
        )
        val success = result.first
        val wallet = result.second
        val error = result.third
        
        onImportingChanged(false)
        
        if (success && wallet != null) {
            onWalletImported(wallet)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onImportingChanged(false)
        onError("Error: ${e.message}")
    }
}

@Composable
fun ImportWalletInfoSection(
    wallet: BitcoinWallet,
    isTestnet: Boolean,
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
            
            // Mnemonic (may be null for private key imports)
            if (wallet.mnemonic != null) {
                InfoRow(
                    label = "Mnemonic:",
                    value = wallet.mnemonic ?: "-",
                    onCopy = onCopyMnemonic,
                    valueColor = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
            }
            
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

fun copyAllWalletDataForImport(
    context: Context,
    wallet: BitcoinWallet,
    isTestnet: Boolean
) {
    val walletData = JSONObject().apply {
        put("network", if (isTestnet) "testnet" else "mainnet")
        wallet.mnemonic?.let { put("mnemonic", it) }
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
