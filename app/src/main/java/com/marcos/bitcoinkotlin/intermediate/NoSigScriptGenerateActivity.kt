package com.marcos.bitcoinkotlin.intermediate

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marcos.bitcoin.BitcoinV1
import com.marcos.bitcoin.models.HTLCAddressResult
import com.marcos.bitcoinkotlin.beginner.SectionHeader
import com.marcos.bitcoinkotlin.beginner.NetworkSelector
import com.marcos.bitcoinkotlin.beginner.copyToClipboard
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * No-Signature Script Generate Activity
 * Corresponds to iOS NoSigScriptGenerateViewController.swift
 */
class NoSigScriptGenerateActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("NoSigScriptGenerate", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoSigScriptGenerateScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun NoSigScriptGenerateScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var lockHeightText by remember { mutableStateOf("2542622") }
    var secretHexText by remember { mutableStateOf("6d79536563726574") }
    var isGenerating by remember { mutableStateOf(false) }
    var noSigScriptResult by remember { mutableStateOf<HTLCAddressResult?>(null) }
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
                text = "No-Sig Script Address Generation",
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
            
            // Lock Height Section
            SectionHeader("Lock Height")
            OutlinedTextField(
                value = lockHeightText,
                onValueChange = { lockHeightText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Enter lock height")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Secret Hex Section
            SectionHeader("Secret Preimage")
            OutlinedTextField(
                value = secretHexText,
                onValueChange = { secretHexText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter secret preimage (HEX format)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true
            )
            Text(
                text = "Tip: Secret preimage must be an even-length hexadecimal string",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Warning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Important: No-sig script means anyone who knows the preimage can spend the funds. Please ensure the preimage remains secret until unlock conditions are met.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Generate Button
            Button(
                onClick = {
                    if (!isGenerating) {
                        val trimmedLockHeight = lockHeightText.trim()
                        if (trimmedLockHeight.isEmpty()) {
                            showToastMessage = "Please enter a valid lock height"
                            return@Button
                        }
                        
                        val lockHeight = trimmedLockHeight.toIntOrNull()
                        if (lockHeight == null || lockHeight <= 0) {
                            showToastMessage = "Please enter a valid lock height"
                            return@Button
                        }
                        
                        val trimmedSecretHex = secretHexText.trim()
                        if (trimmedSecretHex.isEmpty()) {
                            showToastMessage = "Please enter secret preimage"
                            return@Button
                        }
                        
                        scope.launch {
                            performGenerateNoSigScriptAddress(
                                bitcoin = bitcoin,
                                lockHeight = lockHeight,
                                secretHex = trimmedSecretHex,
                                isTestnet = isTestnet,
                                onGeneratingChanged = { isGenerating = it },
                                onResultReceived = { result ->
                                    noSigScriptResult = result
                                },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isGenerating) "Generating..." else "Generate No-Sig Script Address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results Section
            noSigScriptResult?.let { result ->
                NoSigScriptResultSection(
                    result = result,
                    onCopyAddress = {
                        copyToClipboard(context, result.address, "No-Sig Script Address")
                        showToastMessage = "Address copied to clipboard"
                    },
                    onCopyRedeemScript = {
                        copyToClipboard(context, result.redeemScript, "Redeem Script")
                        showToastMessage = "Redeem script copied to clipboard"
                    },
                    onCopyAllData = {
                        val json = JSONObject().apply {
                            put("address", result.address)
                            put("redeemScript", result.redeemScript)
                            put("lockHeight", result.lockHeight)
                            put("secretHex", result.secretHex)
                        }
                        copyToClipboard(context, json.toString(2), "No-Sig Script Data")
                        showToastMessage = "All data copied to clipboard"
                    }
                )
            }
        }
    }
}

@Composable
fun NoSigScriptResultSection(
    result: HTLCAddressResult,
    onCopyAddress: () -> Unit,
    onCopyRedeemScript: () -> Unit,
    onCopyAllData: () -> Unit
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
                text = "Generated No-Sig Script Address",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()
            
            // Address
            Column {
                Text(
                    text = "P2WSH Address:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = result.address,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Button(
                onClick = onCopyAddress,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Address")
            }
            
            HorizontalDivider()
            
            // Redeem Script
            Column {
                Text(
                    text = "Redeem Script:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = result.redeemScript,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Button(
                onClick = onCopyRedeemScript,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Redeem Script")
            }
            
            HorizontalDivider()
            
            // Info Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "When unlocking, only provide preimage and redeem script, no signature required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            HorizontalDivider()
            
            // Copy All Data Button
            OutlinedButton(
                onClick = onCopyAllData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy All JSON")
            }
        }
    }
}

suspend fun performGenerateNoSigScriptAddress(
    bitcoin: BitcoinV1,
    lockHeight: Int,
    secretHex: String,
    isTestnet: Boolean,
    onGeneratingChanged: (Boolean) -> Unit,
    onResultReceived: (HTLCAddressResult) -> Unit,
    onError: (String) -> Unit
) {
    onGeneratingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, HTLCAddressResult?, String?> = bitcoin.generateNoSigScriptAddress(
            lockHeight = lockHeight,
            secretHex = secretHex,
            isTestnet = isTestnet
        )
        val success = result.first
        val noSigResult = result.second
        val error = result.third
        
        onGeneratingChanged(false)
        
        if (success && noSigResult != null) {
            onResultReceived(noSigResult)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onGeneratingChanged(false)
        onError("Error: ${e.message}")
    }
}
