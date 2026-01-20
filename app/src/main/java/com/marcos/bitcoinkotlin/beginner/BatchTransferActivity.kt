package com.marcos.bitcoinkotlin.beginner

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.marcos.bitcoin.models.BatchTransferResult
import com.marcos.bitcoin.models.FeeEstimateResult
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Batch Transfer Activity
 * Corresponds to iOS BatchTransferViewController.swift
 */
class BatchTransferActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("BatchTransfer", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BatchTransferScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun BatchTransferScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var privateKeyText by remember { mutableStateOf("") }
    var isPrivateKeyVisible by remember { mutableStateOf(false) }
    var fromAddressText by remember { mutableStateOf("") }
    var recipientsText by remember { mutableStateOf("tb1q...,1000\ntb1p...,2000") }
    var feeText by remember { mutableStateOf("0.00001") }
    var isTransferring by remember { mutableStateOf(false) }
    var isEstimatingFee by remember { mutableStateOf(false) }
    var feeEstimateResult by remember { mutableStateOf<FeeEstimateResult?>(null) }
    var transferResult by remember { mutableStateOf<BatchTransferResult?>(null) }
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
                text = "Batch Transfer",
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
            SectionHeader("Sender Private Key")
            OutlinedTextField(
                value = privateKeyText,
                onValueChange = { privateKeyText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter private key (HEX format)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true,
                visualTransformation = if (isPrivateKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPrivateKeyVisible = !isPrivateKeyVisible }) {
                        Icon(
                            imageVector = if (isPrivateKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isPrivateKeyVisible) "Hide" else "Show"
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // From Address Section
            SectionHeader("Sender Address")
            Text(
                text = "If not filled, Segwit address will be automatically derived from private key",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = fromAddressText,
                onValueChange = { fromAddressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter sender address (optional)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Recipients Section
            SectionHeader("Recipients List")
            Text(
                text = "Format: address,satoshis - one per line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = recipientsText,
                onValueChange = { recipientsText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = {
                    Text(
                        text = "tb1q...,1000\ntb1p...,2000",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 10
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Fee Section
            SectionHeader("Fee")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("0.00001")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "BTC",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Estimate Fee Button
            Button(
                onClick = {
                    if (!isEstimatingFee) {
                        val trimmedRecipients = recipientsText.trim()
                        if (trimmedRecipients.isEmpty()) {
                            showToastMessage = "Please enter recipients list first"
                            return@Button
                        }
                        
                        scope.launch {
                            // Parse recipients to count valid outputs
                            var outputsCount = 0
                            val lines = trimmedRecipients.split("\n")
                            for (line in lines) {
                                val trimmedLine = line.trim()
                                if (trimmedLine.isEmpty()) continue
                                
                                val parts = trimmedLine.split(",")
                                if (parts.size < 2) continue
                                
                                val address = parts[0].trim()
                                val amountStr = parts[1].trim()
                                
                                if (address.isNotEmpty() && amountStr.toLongOrNull() != null && amountStr.toLong() > 0) {
                                    outputsCount++
                                }
                            }
                            
                            if (outputsCount == 0) {
                                showToastMessage = "No valid recipients found"
                                return@launch
                            }
                            
                            // Add 1 for change address
                            outputsCount += 1
                            
                            // Detect address type from fromAddress or default to segwit
                            val trimmedFromAddress = fromAddressText.trim()
                            val addressType = detectAddressType(trimmedFromAddress)
                            
                            // Estimate inputs count (default to 1)
                            val inputsCount = 1
                            
                            estimateFee(
                                bitcoin = bitcoin,
                                inputsCount = inputsCount,
                                outputsCount = outputsCount,
                                isTestnet = isTestnet,
                                addressType = addressType,
                                onEstimatingChanged = { isEstimatingFee = it },
                                onResultReceived = { result ->
                                    feeEstimateResult = result
                                },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isEstimatingFee,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(if (isEstimatingFee) "Estimating..." else "Estimate Fee")
            }
            
            // Fee Estimate Results
            feeEstimateResult?.let { result ->
                FeeEstimateSection(
                    result = result,
                    onSelectFee = { feeBTC ->
                        feeText = String.format("%.8f", feeBTC)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Execute Batch Transfer Button
            Button(
                onClick = {
                    if (!isTransferring) {
                        val trimmedPrivKey = privateKeyText.trim()
                        if (trimmedPrivKey.isEmpty()) {
                            showToastMessage = "Please enter private key"
                            return@Button
                        }
                        
                        val trimmedFromAddress = fromAddressText.trim()
                        if (trimmedFromAddress.isEmpty()) {
                            showToastMessage = "Please enter sender address (for UTXO query and as change address)"
                            return@Button
                        }
                        
                        val trimmedRecipients = recipientsText.trim()
                        if (trimmedRecipients.isEmpty()) {
                            showToastMessage = "Please enter recipients list"
                            return@Button
                        }
                        
                        // Parse recipients
                        val validRecipients = mutableListOf<Map<String, Any>>()
                        val lines = trimmedRecipients.split("\n")
                        for (line in lines) {
                            val trimmedLine = line.trim()
                            if (trimmedLine.isEmpty()) continue
                            
                            val parts = trimmedLine.split(",")
                            if (parts.size < 2) continue
                            
                            val address = parts[0].trim()
                            val amountStr = parts[1].trim()
                            
                            val amountSats = amountStr.toLongOrNull()
                            if (address.isNotEmpty() && amountSats != null && amountSats > 0) {
                                validRecipients.add(mapOf(
                                    "address" to address,
                                    "value" to amountSats
                                ))
                            }
                        }
                        
                        if (validRecipients.isEmpty()) {
                            showToastMessage = "Parse failed, please check format (address,satoshis - one per line)"
                            return@Button
                        }
                        
                        val trimmedFee = feeText.trim()
                        if (trimmedFee.isEmpty()) {
                            showToastMessage = "Please enter a valid fee"
                            return@Button
                        }
                        
                        val feeBTC = trimmedFee.toDoubleOrNull()
                        if (feeBTC == null || feeBTC <= 0) {
                            showToastMessage = "Please enter a valid fee"
                            return@Button
                        }
                        
                        // Convert to satoshis
                        val feeSats = (feeBTC * 100_000_000).toLong()
                        
                        scope.launch {
                            performBatchTransfer(
                                bitcoin = bitcoin,
                                outputs = validRecipients,
                                feeSats = feeSats,
                                privKeyHex = trimmedPrivKey,
                                isTestnet = isTestnet,
                                fromAddress = trimmedFromAddress,
                                onTransferringChanged = { isTransferring = it },
                                onResultReceived = { result ->
                                    transferResult = result
                                },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isTransferring,
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
                    text = if (isTransferring) "Transferring..." else "Execute Batch Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer Result Section
            transferResult?.let { result ->
                TransferResultSection(
                    txid = result.txid,
                    isTestnet = isTestnet,
                    onCopyTxid = {
                        copyToClipboard(context, result.txid, "Transaction ID")
                        showToastMessage = "Transaction ID copied to clipboard"
                    },
                    onViewTx = {
                        viewTransaction(context, result.txid, isTestnet)
                    }
                )
            }
        }
    }
}

suspend fun performBatchTransfer(
    bitcoin: BitcoinV1,
    outputs: List<Map<String, Any>>,
    feeSats: Long,
    privKeyHex: String,
    isTestnet: Boolean,
    fromAddress: String,
    onTransferringChanged: (Boolean) -> Unit,
    onResultReceived: (BatchTransferResult) -> Unit,
    onError: (String) -> Unit
) {
    onTransferringChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, BatchTransferResult?, String?> = bitcoin.batchTransfer(
            outputs = outputs,
            feeSats = feeSats,
            privKeyHex = privKeyHex,
            isTestnet = isTestnet,
            fromAddress = fromAddress
        )
        val success = result.first
        val batchResult = result.second
        val error = result.third
        
        onTransferringChanged(false)
        
        if (success && batchResult != null) {
            onResultReceived(batchResult)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onTransferringChanged(false)
        onError("Error: ${e.message}")
    }
}

suspend fun estimateFee(
    bitcoin: BitcoinV1,
    inputsCount: Int,
    outputsCount: Int,
    isTestnet: Boolean,
    addressType: String,
    onEstimatingChanged: (Boolean) -> Unit,
    onResultReceived: (FeeEstimateResult) -> Unit,
    onError: (String) -> Unit
) {
    onEstimatingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, FeeEstimateResult?, String?> = bitcoin.estimateFee(
            inputsCount = inputsCount,
            outputsCount = outputsCount,
            isTestnet = isTestnet,
            addressType = addressType
        )
        val success = result.first
        val feeResult = result.second
        val error = result.third
        
        onEstimatingChanged(false)
        
        if (success && feeResult != null) {
            onResultReceived(feeResult)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onEstimatingChanged(false)
        onError("Error: ${e.message}")
    }
}
