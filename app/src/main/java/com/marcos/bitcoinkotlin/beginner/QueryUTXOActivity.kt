package com.marcos.bitcoinkotlin.beginner

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.marcos.bitcoin.BitcoinV1
import com.marcos.bitcoinkotlin.ui.theme.BitcoinKotlinTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Query UTXO Activity
 */
class QueryUTXOActivity : ComponentActivity() {
    
    private lateinit var bitcoin: BitcoinV1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        bitcoin = BitcoinV1(this)
        bitcoin.setup(showLog = true) { success ->
            if (success) {
                android.util.Log.d("QueryUTXO", "Bitcoin library initialized")
            }
        }
        
        setContent {
            BitcoinKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QueryUTXOScreen(bitcoin = bitcoin)
                }
            }
        }
    }
}

// UTXO Data Model
data class UTXOInfo(
    val txHash: String,
    val index: Int,
    val value: Long // In satoshis
) {
    val valueInBTC: Double
        get() = value / 100_000_000.0
}

@Composable
fun QueryUTXOScreen(bitcoin: BitcoinV1) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var isTestnet by remember { mutableStateOf(true) }
    var addressText by remember { mutableStateOf("") }
    var isQuerying by remember { mutableStateOf(false) }
    var utxos by remember { mutableStateOf<List<UTXOInfo>>(emptyList()) }
    var totalBalance by remember { mutableStateOf(0L) }
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
                text = "UTXO Query",
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
            
            // Address Input Section
            SectionHeader("Query Address")
            OutlinedTextField(
                value = addressText,
                onValueChange = { addressText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Enter Bitcoin address",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Query Button
            Button(
                onClick = {
                    if (!isQuerying) {
                        val trimmedAddress = addressText.trim()
                        if (trimmedAddress.isEmpty()) {
                            showToastMessage = "Please enter Bitcoin address"
                            return@Button
                        }
                        
                        // Simple address format validation
                        val addressLower = trimmedAddress.lowercase()
                        val isValidAddress = addressLower.startsWith("bc1") || 
                                            addressLower.startsWith("tb1") || 
                                            addressLower.startsWith("1") || 
                                            addressLower.startsWith("3") ||
                                            addressLower.startsWith("m") ||
                                            addressLower.startsWith("n") ||
                                            addressLower.startsWith("2")
                        
                        if (!isValidAddress) {
                            showToastMessage = "Please enter a valid Bitcoin address"
                            return@Button
                        }
                        
                        // Validate network match
                        val addressIsTestnet = addressLower.startsWith("tb1") || 
                                             addressLower.startsWith("m") || 
                                             addressLower.startsWith("n") || 
                                             addressLower.startsWith("2")
                        val addressIsMainnet = addressLower.startsWith("bc1") || 
                                              addressLower.startsWith("1") || 
                                              addressLower.startsWith("3")
                        
                        if (addressIsTestnet && !isTestnet) {
                            showToastMessage = "This is a testnet address, but you selected mainnet. Please switch to testnet."
                            return@Button
                        } else if (addressIsMainnet && isTestnet) {
                            showToastMessage = "This is a mainnet address, but you selected testnet. Please switch to mainnet."
                            return@Button
                        }
                        
                        scope.launch {
                            queryUTXO(
                                bitcoin = bitcoin,
                                address = trimmedAddress,
                                isTestnet = isTestnet,
                                onQueryingChanged = { isQuerying = it },
                                onResultReceived = { utxoList ->
                                    utxos = utxoList
                                    totalBalance = utxoList.sumOf { it.value }
                                    if (utxoList.isEmpty()) {
                                        showToastMessage = "This address has no UTXO"
                                    } else {
                                        showToastMessage = "Query successful, found ${utxoList.size} UTXO(s)"
                                    }
                                },
                                onError = { showToastMessage = it }
                            )
                        }
                    }
                },
                enabled = !isQuerying,
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
                    text = if (isQuerying) "Querying..." else "Query UTXO",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results Section
            if (utxos.isNotEmpty()) {
                QueryResultSection(
                    utxos = utxos,
                    totalBalance = totalBalance,
                    onCopyTxHash = { txHash ->
                        copyToClipboard(context, txHash, "Transaction Hash")
                        showToastMessage = "Transaction hash copied"
                    }
                )
            }
        }
    }
}

@Composable
fun QueryResultSection(
    utxos: List<UTXOInfo>,
    totalBalance: Long,
    onCopyTxHash: (String) -> Unit
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
                text = "Query Result",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider()
            
            // Total Balance
            Text(
                text = String.format("Total Balance: %.8f BTC", totalBalance / 100_000_000.0),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // UTXO Count
            Text(
                text = "UTXO Count: ${utxos.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider()
            
            // UTXO List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(utxos) { utxo ->
                    UTXORow(
                        utxo = utxo,
                        onCopyTxHash = { onCopyTxHash(utxo.txHash) }
                    )
                }
            }
        }
    }
}

@Composable
fun UTXORow(
    utxo: UTXOInfo,
    onCopyTxHash: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TxHash: ${utxo.txHash}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCopyTxHash,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Index: ${utxo.index}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = String.format("%.8f BTC", utxo.valueInBTC),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

suspend fun queryUTXO(
    bitcoin: BitcoinV1,
    address: String,
    isTestnet: Boolean,
    onQueryingChanged: (Boolean) -> Unit,
    onResultReceived: (List<UTXOInfo>) -> Unit,
    onError: (String) -> Unit
) {
    onQueryingChanged(true)
    
    try {
        // Ensure Bitcoin is initialized
        if (!bitcoin.isSuccess) {
            delay(500)
        }
        
        val result: Triple<Boolean, List<Map<String, Any>>?, String?> = bitcoin.queryUTXO(
            address = address,
            isTestnet = isTestnet
        )
        val success = result.first
        val utxosData = result.second
        val error = result.third
        
        onQueryingChanged(false)
        
        if (success && utxosData != null) {
            val utxoList = utxosData.mapNotNull { utxoMap ->
                try {
                    val txHash = utxoMap["txHash"] as? String ?: return@mapNotNull null
                    val index = (utxoMap["index"] as? Number)?.toInt() ?: return@mapNotNull null
                    val value = (utxoMap["value"] as? Number)?.toLong() ?: return@mapNotNull null
                    UTXOInfo(txHash = txHash, index = index, value = value)
                } catch (e: Exception) {
                    null
                }
            }
            onResultReceived(utxoList)
        } else {
            onError(error ?: "Unknown error")
        }
    } catch (e: Exception) {
        onQueryingChanged(false)
        onError("Error: ${e.message}")
    }
}
