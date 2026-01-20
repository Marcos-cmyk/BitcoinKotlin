package com.marcos.bitcoinkotlin.beginner

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
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Message Sign Activity
 * Corresponds to iOS MessageSignViewController.swift
 */
class MessageSignActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("MessageSign", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MessageSignScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun MessageSignScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var privateKeyText by remember { mutableStateOf("") }
    var isPrivateKeyVisible by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("Hello, Bitcoin!") }
    var addressTypeIndex by remember { mutableStateOf(1) } // Default to Segwit
    var isSigning by remember { mutableStateOf(false) }
    var signatureResult by remember { mutableStateOf<String?>(null) }
    var usedAddress by remember { mutableStateOf<String?>(null) }
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
                text = "Sign Message",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Network Selection
            SectionHeader("Network Type")
            NetworkSelector(
                isTestnet = isTestnet,
                onNetworkChanged = { isTestnet = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Private Key Section
            SectionHeader("Private Key (Hex)")
            OutlinedTextField(
                value = privateKeyText,
                onValueChange = { privateKeyText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter private key (for signing)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                visualTransformation = if (isPrivateKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPrivateKeyVisible = !isPrivateKeyVisible }) {
                        Icon(
                            imageVector = if (isPrivateKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (isPrivateKeyVisible) "Hide" else "Show"
                        )
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true
            )
            Text(
                text = "Tip: Leave empty to use wallet private key from above",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message Section
            SectionHeader("Message Content")
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Enter message to sign",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Address Type Section
            SectionHeader("Address Type")
            AddressTypeSelector(
                selectedIndex = addressTypeIndex,
                onSelectionChanged = { addressTypeIndex = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sign Button
            Button(
                onClick = {
                    if (!isSigning) {
                        val trimmedMessage = messageText.trim()
                        if (trimmedMessage.isEmpty()) {
                            showToastMessage = "Please enter message to sign"
                            return@Button
                        }
                        
                        val trimmedPrivateKey = privateKeyText.trim()
                        val addressType = when (addressTypeIndex) {
                            0 -> "legacy"
                            1 -> "segwit"
                            2 -> "taproot"
                            else -> "segwit"
                        }
                        
                        scope.launch {
                            signMessage(
                                bitcoin = bitcoin,
                                message = trimmedMessage,
                                privKeyHex = if (trimmedPrivateKey.isEmpty()) null else trimmedPrivateKey,
                                addressType = addressType,
                                isTestnet = isTestnet,
                                onSigningChanged = { isSigning = it },
                                onResultReceived = { address, signature ->
                                    usedAddress = address
                                    signatureResult = signature
                                },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isSigning,
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
                    text = if (isSigning) "Signing..." else "Sign Message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Signature Result Section
            signatureResult?.let { signature ->
                usedAddress?.let { address ->
                    SignatureResultSection(
                        address = address,
                        signature = signature,
                        onCopySignature = {
                            copyToClipboard(context, signature, "Signature")
                            showToastMessage = "Signature copied to clipboard"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddressTypeSelector(
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedIndex == 0,
            onClick = { onSelectionChanged(0) },
            label = { Text("Legacy (P2PKH)") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedIndex == 1,
            onClick = { onSelectionChanged(1) },
            label = { Text("Segwit (P2WPKH)") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedIndex == 2,
            onClick = { onSelectionChanged(2) },
            label = { Text("Taproot (P2TR)") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SignatureResultSection(
    address: String,
    signature: String,
    onCopySignature: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section Header
            Text(
                text = "Signature Result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()
            
            // Success Message
            Text(
                text = "✅ Signature successful!",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider()
            
            // Address Used
            Column {
                Text(
                    text = "Address Used:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            HorizontalDivider()
            
            // Signature
            Column {
                Text(
                    text = "Signature (Base64):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = signature,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            HorizontalDivider()
            
            // Copy Signature Button
            Button(
                onClick = onCopySignature,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("📋 Copy Signature")
            }
        }
    }
}

suspend fun signMessage(
    bitcoin: BitcoinV1,
    message: String,
    privKeyHex: String?,
    addressType: String,
    isTestnet: Boolean,
    onSigningChanged: (Boolean) -> Unit,
    onResultReceived: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    onSigningChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, String?, String?> = bitcoin.signMessage(
            message = message,
            privKeyHex = privKeyHex,
            addressType = addressType,
            isTestnet = isTestnet
        )
        val success = result.first
        val address = result.second
        val signature = result.third
        
        onSigningChanged(false)
        
        if (success && address != null && signature != null) {
            onResultReceived(address, signature)
            // Note: Toast message is handled in the UI
        } else {
            onError("Signing failed")
        }
    } catch (e: Exception) {
        onSigningChanged(false)
        onError("Error: ${e.message}")
    }
}
