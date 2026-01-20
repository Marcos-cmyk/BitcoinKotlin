package com.marcos.bitcoinkotlin.advanced

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
import com.marcos.bitcoin.models.MultisigAddressResult
import com.marcos.bitcoinkotlin.beginner.SectionHeader
import com.marcos.bitcoinkotlin.beginner.NetworkSelector
import com.marcos.bitcoinkotlin.beginner.copyToClipboard
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Multisig Generate Activity
 * Corresponds to iOS MultisigGenerateViewController.swift
 */
class MultisigGenerateActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("MultisigGenerate", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MultisigGenerateScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun MultisigGenerateScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var pubkeysText by remember { mutableStateOf("") }
    var thresholdText by remember { mutableStateOf("2") }
    var isGenerating by remember { mutableStateOf(false) }
    var multisigResult by remember { mutableStateOf<MultisigAddressResult?>(null) }
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
                text = "Multisig Address Generation",
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
            
            // Public Keys Section
            SectionHeader("Participant Public Keys")
            OutlinedTextField(
                value = pubkeysText,
                onValueChange = { pubkeysText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Enter public keys (one per line)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 10
            )
            Text(
                text = "Tip: One public key per line (33-byte compressed key starting with 02 or 03)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Threshold Section
            SectionHeader("Required Signatures (Threshold N)")
            OutlinedTextField(
                value = thresholdText,
                onValueChange = { thresholdText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Enter threshold number")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Generate Button
            Button(
                onClick = {
                    if (!isGenerating) {
                        val trimmedPubkeysText = pubkeysText.trim()
                        if (trimmedPubkeysText.isEmpty()) {
                            showToastMessage = "Please enter public key list"
                            return@Button
                        }
                        
                        // Parse public key list (one per line)
                        val pubkeys = trimmedPubkeysText.split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        if (pubkeys.isEmpty()) {
                            showToastMessage = "At least one public key is required"
                            return@Button
                        }
                        
                        val trimmedThreshold = thresholdText.trim()
                        if (trimmedThreshold.isEmpty()) {
                            showToastMessage = "Please enter a valid threshold number"
                            return@Button
                        }
                        
                        val threshold = trimmedThreshold.toIntOrNull()
                        if (threshold == null || threshold <= 0) {
                            showToastMessage = "Please enter a valid threshold number"
                            return@Button
                        }
                        
                        if (threshold > pubkeys.size) {
                            showToastMessage = "Threshold cannot be greater than the number of public keys"
                            return@Button
                        }
                        
                        scope.launch {
                            performGenerateMultisigAddress(
                                bitcoin = bitcoin,
                                threshold = threshold,
                                pubkeys = pubkeys,
                                isTestnet = isTestnet,
                                onGeneratingChanged = { isGenerating = it },
                                onResultReceived = { result ->
                                    multisigResult = result
                                    showToastMessage = "Multisig address generated successfully"
                                },
                                onError = { error -> showToastMessage = error }
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
                    text = if (isGenerating) "Generating..." else "Generate Multisig Address",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results Section
            multisigResult?.let { result ->
                MultisigResultSection(
                    result = result,
                    onCopyScript = {
                        copyToClipboard(context, result.script, "Multisig Script")
                        showToastMessage = "Script copied to clipboard"
                    },
                    onCopyP2sh = {
                        copyToClipboard(context, result.p2shAddress, "P2SH Address")
                        showToastMessage = "P2SH address copied to clipboard"
                    },
                    onCopyP2wsh = {
                        copyToClipboard(context, result.p2wshAddress, "P2WSH Address")
                        showToastMessage = "P2WSH address copied to clipboard"
                    },
                    onCopyAllData = {
                        val json = JSONObject().apply {
                            put("script", result.script)
                            put("p2shAddress", result.p2shAddress)
                            put("p2wshAddress", result.p2wshAddress)
                            put("threshold", result.threshold)
                            put("totalSigners", result.totalSigners)
                        }
                        copyToClipboard(context, json.toString(2), "Multisig Data")
                        showToastMessage = "All info copied to clipboard"
                    }
                )
            }
        }
    }
}

@Composable
fun MultisigResultSection(
    result: MultisigAddressResult,
    onCopyScript: () -> Unit,
    onCopyP2sh: () -> Unit,
    onCopyP2wsh: () -> Unit,
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
                text = "Generation Result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()
            
            // Mode Info
            Text(
                text = "Mode: ${result.threshold}-of-${result.totalSigners}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            
            HorizontalDivider()
            
            // Multisig Script
            Column {
                Text(
                    text = "Multisig Script:",
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
                        text = result.script,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Button(
                onClick = onCopyScript,
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
                Text("Copy Script")
            }
            
            HorizontalDivider()
            
            // P2SH Address
            Column {
                Text(
                    text = "P2SH Address (Legacy):",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = result.p2shAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Button(
                onClick = onCopyP2sh,
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
                Text("Copy P2SH Address")
            }
            
            HorizontalDivider()
            
            // P2WSH Address
            Column {
                Text(
                    text = "P2WSH Address (Segwit):",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = result.p2wshAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Button(
                onClick = onCopyP2wsh,
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
                Text("Copy P2WSH Address")
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
                Text("Copy All Info (JSON)")
            }
        }
    }
}

suspend fun performGenerateMultisigAddress(
    bitcoin: BitcoinV1,
    threshold: Int,
    pubkeys: List<String>,
    isTestnet: Boolean,
    onGeneratingChanged: (Boolean) -> Unit,
    onResultReceived: (MultisigAddressResult) -> Unit,
    onError: (String) -> Unit
) {
    onGeneratingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, MultisigAddressResult?, String?> = bitcoin.generateMultisigAddress(
            threshold = threshold,
            pubkeys = pubkeys,
            isTestnet = isTestnet
        )
        val success = result.first
        val multisigResult = result.second
        val error = result.third
        
        onGeneratingChanged(false)
        
        if (success && multisigResult != null) {
            onResultReceived(multisigResult)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onGeneratingChanged(false)
        onError("Error: ${e.message}")
    }
}
