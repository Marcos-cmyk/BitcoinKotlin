package com.marcos.bitcoinkotlin.advanced

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
import com.marcos.bitcoin.models.MultisigTransferResult
import com.marcos.bitcoinkotlin.beginner.SectionHeader
import com.marcos.bitcoinkotlin.beginner.NetworkSelector
import com.marcos.bitcoinkotlin.beginner.copyToClipboard
import com.marcos.bitcoinkotlin.beginner.viewTransaction
import com.marcos.bitcoinkotlin.intermediate.FeeEstimateSection
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Multisig Transfer Activity
 * Corresponds to iOS MultisigTransferViewController.swift
 */
class MultisigTransferActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("MultisigTransfer", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MultisigTransferScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun MultisigTransferScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var multisigAddressText by remember { mutableStateOf("") }
    var allPubkeysText by remember { mutableStateOf("") }
    var signPrivKeysText by remember { mutableStateOf("") }
    var isSignPrivKeysVisible by remember { mutableStateOf(false) }
    var toAddressText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("3000") }
    var isTransferring by remember { mutableStateOf(false) }
    var isEstimatingFee by remember { mutableStateOf(false) }
    var feeEstimateResult by remember { mutableStateOf<FeeEstimateResult?>(null) }
    var transferResult by remember { mutableStateOf<MultisigTransferResult?>(null) }
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
                text = "Multisig Transfer",
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
            
            // Multisig Address Section
            SectionHeader("Multisig Source Address (P2SH/P2WSH)")
            OutlinedTextField(
                value = multisigAddressText,
                onValueChange = { multisigAddressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter multisig address",
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
            
            // All Pubkeys Section
            SectionHeader("All Participant Public Keys")
            OutlinedTextField(
                value = allPubkeysText,
                onValueChange = { allPubkeysText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
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
                text = "Tip: One public key per line, order must be correct",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sign Private Keys Section
            SectionHeader("Signing Private Keys")
            OutlinedTextField(
                value = signPrivKeysText,
                onValueChange = { signPrivKeysText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                placeholder = {
                    Text(
                        text = "Enter private keys (one per line, HEX format)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                visualTransformation = if (isSignPrivKeysVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isSignPrivKeysVisible = !isSignPrivKeysVisible }) {
                        Icon(
                            imageVector = if (isSignPrivKeysVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isSignPrivKeysVisible) "Hide private keys" else "Show private keys"
                        )
                    }
                },
                maxLines = 10
            )
            Text(
                text = "Tip: One private key per line (HEX format), must meet threshold requirement",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            
            // Amount and Fee Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Amount (BTC)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("0.001") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                
                // Fee
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Fee (Satoshi)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("3000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
            
            // Estimate Fee Button
            OutlinedButton(
                onClick = {
                    val trimmedMultisigAddress = multisigAddressText.trim()
                    if (trimmedMultisigAddress.isEmpty()) {
                        showToastMessage = "Please enter multisig source address first"
                        return@OutlinedButton
                    }
                    
                    val trimmedAllPubkeys = allPubkeysText.trim()
                    if (trimmedAllPubkeys.isEmpty()) {
                        showToastMessage = "Please enter all participant public keys first"
                        return@OutlinedButton
                    }
                    
                    val trimmedToAddress = toAddressText.trim()
                    if (trimmedToAddress.isEmpty()) {
                        showToastMessage = "Please enter recipient address first"
                        return@OutlinedButton
                    }
                    
                    // Parse public keys
                    val allPubkeys = trimmedAllPubkeys.split("\n")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    
                    if (allPubkeys.isEmpty()) {
                        showToastMessage = "At least one public key is required"
                        return@OutlinedButton
                    }
                    
                    val m = allPubkeys.size // Total number of signers
                    
                    // Parse signing private keys to estimate n (threshold)
                    var n = 1
                    val trimmedSignPrivKeys = signPrivKeysText.trim()
                    if (trimmedSignPrivKeys.isNotEmpty()) {
                        val signPrivKeys = trimmedSignPrivKeys.split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        if (signPrivKeys.isNotEmpty()) {
                            n = signPrivKeys.size // Use number of signing keys as threshold estimate
                        }
                    }
                    
                    // Ensure n <= m
                    if (n > m) {
                        n = m
                    }
                    
                    // Multisig unlock transaction typically has:
                    // - 1 input (multisig address UTXO)
                    // - 1 output (recipient address, usually full amount transfer)
                    val inputsCount = 1
                    val outputsCount = 1
                    
                    // Determine address type from multisig address
                    var addressType = "multisig"
                    val addrLower = trimmedMultisigAddress.lowercase()
                    if (addrLower.startsWith("bc1") || addrLower.startsWith("tb1")) {
                        // P2WSH multisig
                        addressType = "multisig"
                    } else if (trimmedMultisigAddress.startsWith("3") || trimmedMultisigAddress.startsWith("2")) {
                        // P2SH multisig (legacy)
                        addressType = "multisig"
                    }
                    
                    scope.launch {
                        isEstimatingFee = true
                        try {
                            // Ensure Bitcoin is initialized
                            if (!bitcoin.isSuccess) {
                                delay(500)
                            }
                            
                            val result: Triple<Boolean, FeeEstimateResult?, String?> = bitcoin.estimateFee(
                                inputsCount = inputsCount,
                                outputsCount = outputsCount,
                                isTestnet = isTestnet,
                                addressType = addressType,
                                n = n,
                                m = m
                            )
                            val success = result.first
                            val feeResult = result.second
                            val error = result.third
                            
                            isEstimatingFee = false
                            
                            if (success && feeResult != null) {
                                feeEstimateResult = feeResult
                            } else {
                                showToastMessage = error ?: "Unknown error"
                            }
                        } catch (e: Exception) {
                            isEstimatingFee = false
                            showToastMessage = "Error: ${e.message}"
                        }
                    }
                },
                enabled = !isEstimatingFee,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEstimatingFee) "Estimating..." else "Estimate Fee",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Fee Estimate Result
            feeEstimateResult?.let { result ->
                FeeEstimateSection(
                    result = result,
                    onSelectFee = { feeBTC ->
                        feeText = ((feeBTC * 100_000_000).toLong()).toString()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer Button
            Button(
                onClick = {
                    if (!isTransferring) {
                        val trimmedMultisigAddress = multisigAddressText.trim()
                        if (trimmedMultisigAddress.isEmpty()) {
                            showToastMessage = "Please enter multisig source address"
                            return@Button
                        }
                        
                        val trimmedAllPubkeys = allPubkeysText.trim()
                        if (trimmedAllPubkeys.isEmpty()) {
                            showToastMessage = "Please enter all participant public keys"
                            return@Button
                        }
                        
                        // Parse public key list (one per line)
                        val allPubkeys = trimmedAllPubkeys.split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        if (allPubkeys.isEmpty()) {
                            showToastMessage = "At least one public key is required"
                            return@Button
                        }
                        
                        val trimmedSignPrivKeys = signPrivKeysText.trim()
                        if (trimmedSignPrivKeys.isEmpty()) {
                            showToastMessage = "Please enter signing private keys"
                            return@Button
                        }
                        
                        // Parse private key list (one per line)
                        val signPrivKeys = trimmedSignPrivKeys.split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        if (signPrivKeys.isEmpty()) {
                            showToastMessage = "At least one private key is required"
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
                            performSendMultisigTransaction(
                                bitcoin = bitcoin,
                                multisigAddress = trimmedMultisigAddress,
                                toAddress = trimmedToAddress,
                                amountSats = amountSats,
                                feeSats = feeSats,
                                allPubkeys = allPubkeys,
                                signPrivKeys = signPrivKeys,
                                isTestnet = isTestnet,
                                onTransferringChanged = { isTransferring = it },
                                onResultReceived = { result ->
                                    transferResult = result
                                    showToastMessage = "Multisig transaction signed and sent successfully"
                                },
                                onError = { error -> showToastMessage = error }
                            )
                        }
                    }
                },
                enabled = !isTransferring,
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
                    text = if (isTransferring) "Signing..." else "Sign and Send Multisig Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer Result Section
            transferResult?.let { result ->
                MultisigTransferResultSection(
                    txid = result.txid,
                    isTestnet = isTestnet,
                    onCopyTxid = {
                        copyToClipboard(context, result.txid, "Transaction ID")
                        showToastMessage = "TXID copied to clipboard"
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
fun MultisigTransferResultSection(
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
            
            // Success Message
            Text(
                text = "🎉 Multisig transaction sent!",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            HorizontalDivider()
            
            // Transaction ID
            Column {
                Text(
                    text = "Transaction Hash (TXID):",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
                Text("View Transaction in Browser")
            }
        }
    }
}

suspend fun performSendMultisigTransaction(
    bitcoin: BitcoinV1,
    multisigAddress: String,
    toAddress: String,
    amountSats: Long,
    feeSats: Long,
    allPubkeys: List<String>,
    signPrivKeys: List<String>,
    isTestnet: Boolean,
    onTransferringChanged: (Boolean) -> Unit,
    onResultReceived: (MultisigTransferResult) -> Unit,
    onError: (String) -> Unit
) {
    onTransferringChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, MultisigTransferResult?, String?> = bitcoin.sendMultisigTransaction(
            multisigAddress = multisigAddress,
            toAddress = toAddress,
            amountSats = amountSats,
            feeSats = feeSats,
            allPubkeys = allPubkeys,
            signPrivKeys = signPrivKeys,
            isTestnet = isTestnet
        )
        val success = result.first
        val transferResult = result.second
        val error = result.third
        
        onTransferringChanged(false)
        
        if (success && transferResult != null) {
            onResultReceived(transferResult)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onTransferringChanged(false)
        onError("Error: ${e.message}")
    }
}
