package com.marcos.bitcoinkotlin.intermediate

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
import com.marcos.bitcoin.models.FeeEstimateResult
import com.marcos.bitcoin.models.HTLCUnlockResult
import com.marcos.bitcoinkotlin.beginner.SectionHeader
import com.marcos.bitcoinkotlin.beginner.NetworkSelector
import com.marcos.bitcoinkotlin.beginner.copyToClipboard
import com.marcos.bitcoinkotlin.beginner.viewTransaction
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * HTLC Unlock Activity
 * Corresponds to iOS HTLCUnlockViewController.swift
 */
class HTLCUnlockActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("HTLCUnlock", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HTLCUnlockScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun HTLCUnlockScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var htlcAddressText by remember { mutableStateOf("") }
    var redeemScriptText by remember { mutableStateOf("") }
    var lockHeightText by remember { mutableStateOf("4811699") }
    var secretHexText by remember { mutableStateOf("6d79536563726574") }
    var privateKeyText by remember { mutableStateOf("") }
    var isPrivateKeyVisible by remember { mutableStateOf(false) }
    var toAddressText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("500") }
    var isUnlocking by remember { mutableStateOf(false) }
    var isEstimatingFee by remember { mutableStateOf(false) }
    var feeEstimateResult by remember { mutableStateOf<FeeEstimateResult?>(null) }
    var unlockResult by remember { mutableStateOf<HTLCUnlockResult?>(null) }
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
                text = "HTLC Unlock & Transfer",
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
            
            // HTLC Address Section
            SectionHeader("HTLC Source Address")
            OutlinedTextField(
                value = htlcAddressText,
                onValueChange = { htlcAddressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter HTLC source address (P2WSH)",
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
            
            // Redeem Script Section
            SectionHeader("Redeem Script")
            OutlinedTextField(
                value = redeemScriptText,
                onValueChange = { redeemScriptText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = {
                    Text(
                        text = "Enter redeem script (HEX format)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Lock Height and Secret Section (Side by Side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Lock Height
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Lock Height",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = lockHeightText,
                        onValueChange = { lockHeightText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Lock height")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // Secret Preimage
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Secret Preimage",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = secretHexText,
                        onValueChange = { secretHexText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Secret preimage (HEX)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Private Key Section
            SectionHeader("Owner Private Key")
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
            
            // Recipient Address Section
            SectionHeader("Recipient Address")
            OutlinedTextField(
                value = toAddressText,
                onValueChange = { toAddressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter recipient address",
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
            
            // Amount Section
            SectionHeader("Transfer Amount")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("0.0")
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
                        Text("0")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Sats",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Estimate Fee Button
            Button(
                onClick = {
                    if (!isEstimatingFee) {
                        val trimmedHtlcAddress = htlcAddressText.trim()
                        if (trimmedHtlcAddress.isEmpty()) {
                            showToastMessage = "Please enter HTLC source address first"
                            return@Button
                        }
                        
                        val trimmedToAddress = toAddressText.trim()
                        if (trimmedToAddress.isEmpty()) {
                            showToastMessage = "Please enter recipient address first"
                            return@Button
                        }
                        
                        scope.launch {
                            // HTLC unlock transaction typically has:
                            // - 1 input (HTLC address UTXO)
                            // - 1 output (recipient address, full amount transfer)
                            val inputsCount = 1
                            val outputsCount = 1
                            
                            // HTLC addresses are typically P2WSH (segwit)
                            val addressType = "segwit"
                            
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
                        val feeSats = (feeBTC * 100_000_000).toLong()
                        feeText = feeSats.toString()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Unlock Button
            Button(
                onClick = {
                    if (!isUnlocking) {
                        val trimmedHtlcAddress = htlcAddressText.trim()
                        if (trimmedHtlcAddress.isEmpty()) {
                            showToastMessage = "Please enter HTLC source address"
                            return@Button
                        }
                        
                        val trimmedRedeemScript = redeemScriptText.trim()
                        if (trimmedRedeemScript.isEmpty()) {
                            showToastMessage = "Please enter redeem script"
                            return@Button
                        }
                        
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
                        
                        val trimmedPrivKey = privateKeyText.trim()
                        if (trimmedPrivKey.isEmpty()) {
                            showToastMessage = "Please enter private key"
                            return@Button
                        }
                        
                        val trimmedToAddress = toAddressText.trim()
                        if (trimmedToAddress.isEmpty()) {
                            showToastMessage = "Please enter recipient address"
                            return@Button
                        }
                        
                        val trimmedAmount = amountText.trim()
                        if (trimmedAmount.isEmpty()) {
                            showToastMessage = "Please enter a valid transfer amount"
                            return@Button
                        }
                        
                        val amountBTC = trimmedAmount.toDoubleOrNull()
                        if (amountBTC == null || amountBTC <= 0) {
                            showToastMessage = "Please enter a valid transfer amount"
                            return@Button
                        }
                        
                        val trimmedFee = feeText.trim()
                        if (trimmedFee.isEmpty()) {
                            showToastMessage = "Please enter a valid fee"
                            return@Button
                        }
                        
                        val feeSats = trimmedFee.toLongOrNull()
                        if (feeSats == null || feeSats <= 0) {
                            showToastMessage = "Please enter a valid fee"
                            return@Button
                        }
                        
                        val amountSats = (amountBTC * 100_000_000).toLong()
                        
                        scope.launch {
                            performUnlockHtlcAddress(
                                bitcoin = bitcoin,
                                htlcAddress = trimmedHtlcAddress,
                                toAddress = trimmedToAddress,
                                amountSats = amountSats,
                                feeSats = feeSats,
                                privKeyHex = trimmedPrivKey,
                                lockHeight = lockHeight,
                                secretHex = trimmedSecretHex,
                                redeemScript = trimmedRedeemScript,
                                isTestnet = isTestnet,
                                onUnlockingChanged = { isUnlocking = it },
                                onResultReceived = { result ->
                                    unlockResult = result
                                    showToastMessage = "HTLC unlocked and transferred successfully"
                                },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isUnlocking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isUnlocking) "Unlocking..." else "Unlock and Broadcast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer Result Section
            unlockResult?.let { result ->
                HTLCUnlockResultSection(
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

@Composable
fun FeeEstimateSection(
    result: FeeEstimateResult,
    onSelectFee: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estimated Fee (select one to auto-fill)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Low Fee Button
            OutlinedButton(
                onClick = { onSelectFee(result.lowInBTC) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = String.format("Low Fee: %.8f BTC (~%d satoshis)", result.lowInBTC, result.low),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Medium Fee Button
            OutlinedButton(
                onClick = { onSelectFee(result.mediumInBTC) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = String.format("Medium Fee: %.8f BTC (~%d satoshis)", result.mediumInBTC, result.medium),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // High Fee Button
            OutlinedButton(
                onClick = { onSelectFee(result.highInBTC) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = String.format("High Fee: %.8f BTC (~%d satoshis)", result.highInBTC, result.high),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun HTLCUnlockResultSection(
    txid: String,
    isTestnet: Boolean,
    onCopyTxid: () -> Unit,
    onViewTx: () -> Unit
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
                text = "Transfer Result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()
            
            // Transaction ID
            Column {
                Text(
                    text = "Transaction ID (TXID):",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = txid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Button(
                onClick = onCopyTxid,
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
                Text("Copy TXID")
            }
            
            OutlinedButton(
                onClick = onViewTx,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Transaction")
            }
        }
    }
}

suspend fun performUnlockHtlcAddress(
    bitcoin: BitcoinV1,
    htlcAddress: String,
    toAddress: String,
    amountSats: Long,
    feeSats: Long,
    privKeyHex: String,
    lockHeight: Int,
    secretHex: String,
    redeemScript: String,
    isTestnet: Boolean,
    onUnlockingChanged: (Boolean) -> Unit,
    onResultReceived: (HTLCUnlockResult) -> Unit,
    onError: (String) -> Unit
) {
    onUnlockingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, HTLCUnlockResult?, String?> = bitcoin.unlockHtlcAddress(
            htlcAddress = htlcAddress,
            toAddress = toAddress,
            amountSats = amountSats,
            feeSats = feeSats,
            privKeyHex = privKeyHex,
            lockHeight = lockHeight,
            secretHex = secretHex,
            redeemScript = redeemScript,
            isTestnet = isTestnet
        )
        val success = result.first
        val unlockResult = result.second
        val error = result.third
        
        onUnlockingChanged(false)
        
        if (success && unlockResult != null) {
            onResultReceived(unlockResult)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onUnlockingChanged(false)
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
