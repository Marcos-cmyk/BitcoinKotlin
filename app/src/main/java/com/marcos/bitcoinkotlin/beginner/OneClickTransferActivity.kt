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
import com.marcos.bitcoin.models.FeeEstimateResult
import com.marcos.bitcoin.models.OneClickTransferResult
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One Click Transfer Activity
 * Corresponds to iOS OneClickTransferViewController.swift
 */
class OneClickTransferActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("OneClickTransfer", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OneClickTransferScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun OneClickTransferScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var privateKeyText by remember { mutableStateOf("") }
    var isPrivateKeyVisible by remember { mutableStateOf(false) }
    var fromAddressText by remember { mutableStateOf("") }
    var toAddressText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("0.00001") }
    var isTransferring by remember { mutableStateOf(false) }
    var isEstimatingFee by remember { mutableStateOf(false) }
    var feeEstimateResult by remember { mutableStateOf<FeeEstimateResult?>(null) }
    var transferResult by remember { mutableStateOf<OneClickTransferResult?>(null) }
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
                text = "One-click Transfer",
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // From Address Section
            SectionHeader("Sender Address (Optional)")
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
            
            // To Address Section
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
            Text(
                text = "Suggested fee: 0.00001 - 0.0001 BTC (approximately 1000 - 10000 satoshis)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Estimate Fee Button
            Button(
                onClick = {
                    if (!isEstimatingFee) {
                        val trimmedToAddress = toAddressText.trim()
                        if (trimmedToAddress.isEmpty()) {
                            showToastMessage = "Please enter recipient address first"
                            return@Button
                        }
                        
                        scope.launch {
                            // Detect address type from fromAddress or default to segwit
                            val trimmedFromAddress = fromAddressText.trim()
                            val addressType = detectAddressType(trimmedFromAddress)
                            
                            estimateFee(
                                bitcoin = bitcoin,
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
                        showToastMessage = "Fee selected and filled"
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer Button
            Button(
                onClick = {
                    if (!isTransferring) {
                        val trimmedPrivateKey = privateKeyText.trim()
                        if (trimmedPrivateKey.isEmpty()) {
                            showToastMessage = "Please enter private key"
                            return@Button
                        }
                        
                        val trimmedToAddress = toAddressText.trim()
                        if (trimmedToAddress.isEmpty()) {
                            showToastMessage = "Please enter recipient address"
                            return@Button
                        }
                        
                        val amountBTC = amountText.trim().toDoubleOrNull()
                        if (amountBTC == null || amountBTC <= 0) {
                            showToastMessage = "Please enter a valid transfer amount"
                            return@Button
                        }
                        
                        val feeBTC = feeText.trim().toDoubleOrNull()
                        if (feeBTC == null || feeBTC <= 0) {
                            showToastMessage = "Please enter a valid fee"
                            return@Button
                        }
                        
                        val amountSats = (amountBTC * 100_000_000).toLong()
                        val feeSats = (feeBTC * 100_000_000).toLong()
                        val trimmedFromAddress = fromAddressText.trim().takeIf { it.isNotEmpty() }
                        
                        scope.launch {
                            oneClickTransfer(
                                bitcoin = bitcoin,
                                privKeyHex = trimmedPrivateKey,
                                toAddress = trimmedToAddress,
                                amountSats = amountSats,
                                feeSats = feeSats,
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
                    text = if (isTransferring) "Transferring..." else "Execute Transfer",
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
fun TransferResultSection(
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
                    text = "Transaction ID:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = txid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            HorizontalDivider()
            
            // Copy Transaction ID Button
            Button(
                onClick = onCopyTxid,
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
                Text("Copy Transaction ID")
            }
            
            // View Transaction Button
            Button(
                onClick = onViewTx,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("View Transaction")
            }
        }
    }
}

fun detectAddressType(address: String): String {
    if (address.isEmpty()) {
        return "segwit" // Default
    }
    
    val addrLower = address.lowercase()
    return when {
        addrLower.startsWith("bc1p") || addrLower.startsWith("tb1p") -> "taproot"
        addrLower.startsWith("bc1q") || addrLower.startsWith("tb1q") || 
        addrLower.startsWith("bc1") || addrLower.startsWith("tb1") -> "segwit"
        address.startsWith("1") || address.startsWith("3") || 
        address.startsWith("m") || address.startsWith("n") || 
        address.startsWith("2") -> "legacy"
        else -> "segwit" // Default
    }
}

suspend fun estimateFee(
    bitcoin: BitcoinV1,
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
            inputsCount = 1,
            outputsCount = 2,
            isTestnet = isTestnet,
            addressType = addressType
        )
        val success = result.first
        val estimateResult = result.second
        val error = result.third
        
        onEstimatingChanged(false)
        
        if (success && estimateResult != null) {
            onResultReceived(estimateResult)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onEstimatingChanged(false)
        onError("Error: ${e.message}")
    }
}

suspend fun oneClickTransfer(
    bitcoin: BitcoinV1,
    privKeyHex: String,
    toAddress: String,
    amountSats: Long,
    feeSats: Long,
    isTestnet: Boolean,
    fromAddress: String?,
    onTransferringChanged: (Boolean) -> Unit,
    onResultReceived: (OneClickTransferResult) -> Unit,
    onError: (String) -> Unit
) {
    onTransferringChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, OneClickTransferResult?, String?> = bitcoin.oneClickTransfer(
            privKeyHex = privKeyHex,
            toAddress = toAddress,
            amountSats = amountSats,
            feeSats = feeSats,
            isTestnet = isTestnet,
            fromAddress = fromAddress
        )
        val success = result.first
        val transferResult = result.second
        val error = result.third
        
        onTransferringChanged(false)
        
        if (success && transferResult != null) {
            onResultReceived(transferResult)
            // Note: Success toast is handled in UI
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onTransferringChanged(false)
        onError("Error: ${e.message}")
    }
}

fun viewTransaction(context: Context, txid: String, isTestnet: Boolean) {
    val baseURL = if (isTestnet) "https://blockstream.info/testnet/tx" else "https://blockstream.info/tx"
    val urlString = "$baseURL/$txid"
    
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("OneClickTransfer", "Failed to open transaction URL", e)
    }
}
