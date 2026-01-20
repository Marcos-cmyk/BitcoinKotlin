package com.marcos.bitcoinkotlin.intermediate

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
 * No-Signature Script Unlock Activity
 * Corresponds to iOS NoSigScriptUnlockViewController.swift
 */
class NoSigScriptUnlockActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("NoSigScriptUnlock", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoSigScriptUnlockScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

@Composable
fun NoSigScriptUnlockScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var noSigAddressText by remember { mutableStateOf("") }
    var redeemScriptText by remember { mutableStateOf("") }
    var lockHeightText by remember { mutableStateOf("2542622") }
    var secretHexText by remember { mutableStateOf("6d79536563726574") }
    var toAddressText by remember { mutableStateOf("") }
    var changeAddressText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("300") }
    var isUnlocking by remember { mutableStateOf(false) }
    var isEstimatingFee by remember { mutableStateOf(false) }
    var feeEstimateResult by remember { mutableStateOf<FeeEstimateResult?>(null) }
    var unlockResult by remember { mutableStateOf<HTLCUnlockResult?>(null) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }
    
    // 检查找零地址是否等于源地址
    val isChangeAddressSameAsSource = remember(changeAddressText, noSigAddressText) {
        changeAddressText.trim().isNotEmpty() && 
        changeAddressText.trim().lowercase() == noSigAddressText.trim().lowercase()
    }
    
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
                text = "No-Sig Script: Unlock & Transfer",
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
            
            // No-Sig Script Address Section
            SectionHeader("No-Sig Script Source Address")
            OutlinedTextField(
                value = noSigAddressText,
                onValueChange = { noSigAddressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter no-sig script source address (P2WSH)",
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
            
            // Hint Card
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
                        text = "💡",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Tip: No-sig script unlock does not require private key or signature, only preimage is needed. Witness stack structure: [secretHex, redeemScript]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
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
            
            // Change Address Section (Optional)
            SectionHeader("Change Address (Optional)")
            OutlinedTextField(
                value = changeAddressText,
                onValueChange = { changeAddressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter change address (leave empty to use recipient address)",
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
                text = "Tip: Change will be sent to this address if change amount ≥ 546 sats. Use a regular address you control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Warning Card if change address equals source address
            if (isChangeAddressSameAsSource) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Warning: Change Address Same as Source Address",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "If you use the source address as change address:\n" +
                                   "• Change will be locked again with the same conditions\n" +
                                   "• You'll need to provide preimage and meet time lock again\n" +
                                   "• You'll lose immediate control of the change\n\n" +
                                   "💡 Recommendation: Use a regular address you control (P2PKH, P2WPKH, or P2TR) so you can use the change immediately without unlocking again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
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
                        val trimmedNoSigAddress = noSigAddressText.trim()
                        if (trimmedNoSigAddress.isEmpty()) {
                            showToastMessage = "Please enter no-sig script source address first"
                            return@Button
                        }
                        
                        val trimmedToAddress = toAddressText.trim()
                        if (trimmedToAddress.isEmpty()) {
                            showToastMessage = "Please enter recipient address first"
                            return@Button
                        }
                        
                        scope.launch {
                            // No-sig script unlock transaction typically has:
                            // - 1 input (no-sig script address UTXO)
                            // - 1 output (recipient address, usually full amount transfer)
                            val inputsCount = 1
                            val outputsCount = 1
                            
                            // No-sig script addresses are typically P2WSH (segwit)
                            val addressType = "segwit"
                            
                            estimateFee(
                                bitcoin = bitcoin,
                                inputsCount = inputsCount,
                                outputsCount = outputsCount,
                                isTestnet = isTestnet,
                                addressType = addressType,
                                onEstimatingChanged = { estimating -> isEstimatingFee = estimating },
                                onResultReceived = { result ->
                                    feeEstimateResult = result
                                },
                                onError = { error -> showToastMessage = error }
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
                        val trimmedNoSigAddress = noSigAddressText.trim()
                        if (trimmedNoSigAddress.isEmpty()) {
                            showToastMessage = "Please enter no-sig script source address"
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
                        
                        // 验证找零地址不能等于源地址（提交时禁止 - 双重保障）
                        val trimmedChangeAddress = changeAddressText.trim()
                        if (trimmedChangeAddress.isNotEmpty()) {
                            // 检查找零地址是否等于源地址
                            if (trimmedChangeAddress.lowercase() == trimmedNoSigAddress.lowercase()) {
                                showToastMessage = "❌ Change address cannot be the same as source address!\n\n" +
                                                 "Reason: If change is sent back to the source address, it will be " +
                                                 "locked again and require the same unlock conditions (preimage + time lock).\n\n" +
                                                 "Solution: Please enter a different address that you control " +
                                                 "(e.g., your regular wallet address: P2PKH, P2WPKH, or P2TR)."
                                return@Button  // 禁止提交
                            }
                            
                            // 检查找零地址是否等于接收地址（虽然技术上可以，但不推荐）
                            if (trimmedChangeAddress.lowercase() == trimmedToAddress.lowercase()) {
                                // 这只是一个提示，不禁止（因为找零发送到接收地址是可以的，只是用户可能不想要）
                                // 如果需要禁止，可以取消下面的注释
                                // showToastMessage = "⚠️ Change address is the same as recipient address. Change will also be sent to recipient."
                                // return@Button
                            }
                        }
                        
                        // 如果找零地址为空，传递 null；否则传递找零地址
                        val finalChangeAddress = if (trimmedChangeAddress.isEmpty()) null else trimmedChangeAddress
                        
                        scope.launch {
                            performUnlockNoSigScriptAddress(
                                bitcoin = bitcoin,
                                noSigAddress = trimmedNoSigAddress,
                                toAddress = trimmedToAddress,
                                amountSats = amountSats,
                                feeSats = feeSats,
                                lockHeight = lockHeight,
                                secretHex = trimmedSecretHex,
                                redeemScript = trimmedRedeemScript,
                                isTestnet = isTestnet,
                                changeAddress = finalChangeAddress,
                                onUnlockingChanged = { unlocking -> isUnlocking = unlocking },
                                onResultReceived = { result ->
                                    unlockResult = result
                                    showToastMessage = "No-sig script unlocked and transferred successfully"
                                },
                                onError = { error -> showToastMessage = error }
                            )
                        }
                    }
                },
                enabled = !isUnlocking,
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
                    text = if (isUnlocking) "Unlocking..." else "Unlock and Broadcast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Transfer Result Section
            unlockResult?.let { result ->
                NoSigScriptUnlockResultSection(
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
fun NoSigScriptUnlockResultSection(
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
                    color = MaterialTheme.colorScheme.error
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

suspend fun performUnlockNoSigScriptAddress(
    bitcoin: BitcoinV1,
    noSigAddress: String,
    toAddress: String,
    amountSats: Long,
    feeSats: Long,
    lockHeight: Int,
    secretHex: String,
    redeemScript: String,
    isTestnet: Boolean,
    changeAddress: String? = null,
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
        
        val result: Triple<Boolean, HTLCUnlockResult?, String?> = bitcoin.unlockNoSigScriptAddress(
            noSigAddress = noSigAddress,
            toAddress = toAddress,
            amountSats = amountSats,
            feeSats = feeSats,
            lockHeight = lockHeight,
            secretHex = secretHex,
            redeemScript = redeemScript,
            isTestnet = isTestnet,
            changeAddress = changeAddress
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

