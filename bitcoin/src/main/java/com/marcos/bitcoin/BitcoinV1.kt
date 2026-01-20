package com.marcos.bitcoin

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.marcos.bitcoin.models.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * BitcoinV1 - Main class for Bitcoin operations
 * Provides async/await support via Kotlin Coroutines
 * Mirrors iOS Bitcoin_V1.swift functionality
 */
class BitcoinV1(private val context: Context) {
    
    private var webView: WebView? = null
    private var bridge: WebViewJavascriptBridge? = null
    
    /**
     * Whether Bitcoin library initialization succeeded
     */
    var isSuccess: Boolean = false
        private set
    
    /**
     * Whether to show logs
     */
    var showLog: Boolean = true
        private set
    
    /**
     * Callback when initialization completes
     */
    private var onCompleted: ((Boolean) -> Unit)? = null

    init {
        initializeWebView()
    }

    /**
     * Initialize WebView and Bridge
     */
    private fun initializeWebView() {
        webView = WebView(context).apply {
            // JavaScript is required for communication with the Bitcoin library
            // This is safe as we only load local HTML files from assets
            @Suppress("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (showLog) {
                        Log.d(TAG, "WebView page finished loading: $url")
                    }
                }
                
                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (showLog && error != null && request != null) {
                        Log.e(TAG, "WebView error: ${error.description} (code: ${error.errorCode}, url: ${request.url})")
                    }
                }
            }
        }
        
        bridge = WebViewJavascriptBridge(webView!!, isHookConsole = showLog)
        
        // Set console pipe closure
        bridge?.consolePipeClosure = { message ->
            if (showLog) {
                Log.d(TAG, message?.toString() ?: "")
            }
        }
    }

    /**
     * Setup Bitcoin library
     * - Parameters:
     *   - showLog: Whether to show logs (default: true)
     *   - onCompleted: Callback when initialization completes (default: null)
     */
    fun setup(
        showLog: Boolean = true,
        onCompleted: ((Boolean) -> Unit)? = null
    ) {
        this.showLog = showLog
        this.onCompleted = onCompleted
        
        // Register handler for JS to notify Native when ready
        bridge?.register("generateBitcoin") { _, _ ->
            isSuccess = true
            onCompleted?.invoke(true)
        }
        
        // Load HTML file from assets
        // Resources are in bitcoin/src/main/assets/ (library module assets)
        // Android will merge library assets into app assets during build
        webView?.loadUrl("file:///android_asset/index.html")
    }

    // MARK: createAccount
    /**
     * Generate wallet account
     * - Parameters:
     *   - mnemonicLength: Mnemonic length (128/160/192/224/256, corresponding to 12/15/18/21/24 words), default 128
     *   - isTestnet: Whether to use testnet, default true
     *   - language: Mnemonic language (default: "english")
     *   - onCompleted: Completion callback with parameters (success, wallet, errorMessage)
     */
    fun createAccount(
        mnemonicLength: Int = 128,
        isTestnet: Boolean = true,
        language: String = "english",
        onCompleted: ((Boolean, BitcoinWallet?, String?) -> Unit)? = null
    ) {
        // Map language parameter to bip39 library supported key names
        val mappedLanguage = mapLanguageToBip39(language)
        
        if (showLog) {
            Log.d(TAG, "createAccount - Original language: $language, Mapped language: $mappedLanguage")
        }
        
        // Convert JSONObject to Map to match iOS behavior
        val params = mapOf(
            "mnemonicLength" to mnemonicLength,
            "isTestnet" to isTestnet,
            "language" to mappedLanguage
        )
        
        if (showLog) {
            Log.d(TAG, "createAccount - Params: $params")
            Log.d(TAG, "createAccount - Language value type: ${mappedLanguage::class.java.simpleName}, value: '$mappedLanguage'")
        }
        
        bridge?.call("GenerateWallet", params) { response ->
            if (showLog) {
                Log.d(TAG, "GenerateWallet response = $response")
            }
            
            try {
                // Handle different response types
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        // Convert Map to JSONObject recursively
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val wallet = BitcoinWallet.fromJson(data)
                
                if (wallet != null) {
                    onCompleted?.invoke(true, wallet, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse wallet data")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing GenerateWallet response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Create wallet account using suspend function (Coroutines)
     * - Parameters:
     *   - mnemonicLength: Mnemonic length in bits (128/160/192/224/256, default: 128)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - language: Mnemonic language (default: "english")
     * - Returns: Triple containing (success: Boolean, wallet: BitcoinWallet?, error: String?)
     */
    suspend fun createAccount(
        mnemonicLength: Int = 128,
        isTestnet: Boolean = true,
        language: String = "english"
    ): Triple<Boolean, BitcoinWallet?, String?> = suspendCancellableCoroutine { continuation ->
        createAccount(mnemonicLength, isTestnet, language) { success, wallet, error ->
            continuation.resume(Triple(success, wallet, error))
        }
    }

    // MARK: importAccountFromMnemonic
    /**
     * Import wallet account from mnemonic
     * - Parameters:
     *   - mnemonic: Mnemonic string (12 or 24 words)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - language: Mnemonic language (optional)
     *   - onCompleted: Completion callback with parameters (success, wallet, errorMessage)
     */
    fun importAccountFromMnemonic(
        mnemonic: String,
        isTestnet: Boolean = true,
        language: String? = null,
        onCompleted: ((Boolean, BitcoinWallet?, String?) -> Unit)? = null
    ) {
        val params = mutableMapOf(
            "mnemonic" to mnemonic,
            "isTestnet" to isTestnet
        )
        
        language?.let {
            params["language"] = it
        }
        
        bridge?.call("ImportWalletFromMnemonic", params) { response ->
            if (showLog) {
                Log.d(TAG, "ImportWalletFromMnemonic response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val wallet = BitcoinWallet.fromJson(data)
                
                if (wallet != null) {
                    onCompleted?.invoke(true, wallet, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse wallet data")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ImportWalletFromMnemonic response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Import wallet account from mnemonic using suspend function (Coroutines)
     */
    suspend fun importAccountFromMnemonic(
        mnemonic: String,
        isTestnet: Boolean = true,
        language: String? = null
    ): Triple<Boolean, BitcoinWallet?, String?> = suspendCancellableCoroutine { continuation ->
        importAccountFromMnemonic(mnemonic, isTestnet, language) { success, wallet, error ->
            continuation.resume(Triple(success, wallet, error))
        }
    }
    
    // MARK: importAccountFromPrivateKey
    /**
     * Import wallet account from private key
     * - Parameters:
     *   - privateKey: Private key string (64 hexadecimal characters)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, wallet, errorMessage)
     */
    fun importAccountFromPrivateKey(
        privateKey: String,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, BitcoinWallet?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "privateKey" to privateKey,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("ImportWalletFromPrivateKey", params) { response ->
            if (showLog) {
                Log.d(TAG, "ImportWalletFromPrivateKey response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val wallet = BitcoinWallet.fromJson(data)
                
                if (wallet != null) {
                    onCompleted?.invoke(true, wallet, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse wallet data")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ImportWalletFromPrivateKey response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Import wallet account from private key using suspend function (Coroutines)
     */
    suspend fun importAccountFromPrivateKey(
        privateKey: String,
        isTestnet: Boolean = true
    ): Triple<Boolean, BitcoinWallet?, String?> = suspendCancellableCoroutine { continuation ->
        importAccountFromPrivateKey(privateKey, isTestnet) { success, wallet, error ->
            continuation.resume(Triple(success, wallet, error))
        }
    }

    // MARK: validateAddress
    /**
     * Validate Bitcoin address
     * - Parameters:
     *   - address: Bitcoin address string
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun validateAddress(
        address: String,
        onCompleted: ((Boolean, AddressValidationResult?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "address" to address
        )
        
        bridge?.call("ValidateAddress", params) { response ->
            if (showLog) {
                Log.d(TAG, "ValidateAddress response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = AddressValidationResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse validation result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing ValidateAddress response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Validate Bitcoin address using suspend function (Coroutines)
     */
    suspend fun validateAddress(
        address: String
    ): Triple<Boolean, AddressValidationResult?, String?> = suspendCancellableCoroutine { continuation ->
        validateAddress(address) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: signMessage
    /**
     * BIP322 message signing
     * - Parameters:
     *   - message: Message to sign
     *   - privKeyHex: Private key (Hex format, optional)
     *   - addressType: Address type ("legacy", "segwit", "taproot")
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, address, signature, errorMessage)
     */
    fun signMessage(
        message: String,
        privKeyHex: String? = null,
        addressType: String,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, String?, String?, String?) -> Unit)? = null
    ) {
        val params = mutableMapOf(
            "message" to message,
            "addressType" to addressType,
            "isTestnet" to isTestnet
        )
        
        privKeyHex?.let {
            params["privKeyHex"] = it
        }
        
        bridge?.call("SignMessage", params) { response ->
            if (showLog) {
                Log.d(TAG, "SignMessage response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, null, error)
                    return@call
                }
                
                val address: String? = if (json.has("address") && !json.isNull("address")) {
                    json.getString("address")
                } else {
                    null
                }
                
                val signature: String? = if (json.has("signature") && !json.isNull("signature")) {
                    json.getString("signature")
                } else {
                    null
                }
                
                if (address != null && signature != null) {
                    onCompleted?.invoke(true, address, signature, null)
                } else {
                    onCompleted?.invoke(false, null, null, "Missing 'address' or 'signature' field in response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SignMessage response", e)
                onCompleted?.invoke(false, null, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Sign message using suspend function (Coroutines)
     * Returns: Triple containing (success: Boolean, address: String?, signature: String?)
     * Note: error message is not included in return value, check success flag instead
     */
    suspend fun signMessage(
        message: String,
        privKeyHex: String? = null,
        addressType: String,
        isTestnet: Boolean = true
    ): Triple<Boolean, String?, String?> = suspendCancellableCoroutine { continuation ->
        signMessage(message, privKeyHex, addressType, isTestnet) { success, address, signature, _ ->
            continuation.resume(Triple(success, address, signature))
        }
    }
    
    // MARK: verifyMessage
    /**
     * BIP322 message verification
     * - Parameters:
     *   - message: Original message
     *   - signature: Signature (Base64 format)
     *   - address: Address claimed by signer
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, isValid, errorMessage)
     */
    fun verifyMessage(
        message: String,
        signature: String,
        address: String,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, Boolean, String?) -> Unit)? = null
    ) {
        // Clean signature: remove all whitespace and newlines
        // Base64 should only contain A-Z, a-z, 0-9, +, /, and = (for padding)
        var cleanedSignature = signature.trim()
            .replace(Regex("\\s+"), "") // Remove all whitespace
            .replace(Regex("[^A-Za-z0-9+/=]"), "") // Remove any non-Base64 characters
        
        // Base64 strings must have length that is a multiple of 4
        // Check if padding is needed
        val baseLength = cleanedSignature.replace(Regex("=+$"), "").length
        val remainder = baseLength % 4
        if (remainder != 0) {
            // Remove any existing padding first, then add correct padding
            cleanedSignature = cleanedSignature.replace(Regex("=+$"), "")
            cleanedSignature += "=".repeat(4 - remainder)
        }
        
        if (showLog) {
            Log.d(TAG, "verifyMessage - Original signature (first 100): ${signature.take(100)}")
            Log.d(TAG, "verifyMessage - Cleaned signature (first 100): ${cleanedSignature.take(100)}")
            Log.d(TAG, "verifyMessage - Original length: ${signature.length}, Cleaned length: ${cleanedSignature.length}")
            Log.d(TAG, "verifyMessage - Is length multiple of 4: ${cleanedSignature.length % 4 == 0}")
        }
        
        val params = mapOf(
            "message" to message,
            "signature" to cleanedSignature,
            "address" to address,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("VerifyMessage", params) { response ->
            if (showLog) {
                Log.d(TAG, "VerifyMessage response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, false, error)
                    return@call
                }
                
                val isValid = json.getBoolean("isValid")
                onCompleted?.invoke(true, isValid, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing VerifyMessage response", e)
                onCompleted?.invoke(false, false, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Verify message signature using suspend function (Coroutines)
     */
    suspend fun verifyMessage(
        message: String,
        signature: String,
        address: String,
        isTestnet: Boolean = true
    ): Triple<Boolean, Boolean, String?> = suspendCancellableCoroutine { continuation ->
        verifyMessage(message, signature, address, isTestnet) { success, isValid, error ->
            continuation.resume(Triple(success, isValid, error))
        }
    }

    // MARK: oneClickTransfer
    /**
     * One-click transfer (automatically fetch balance from network, sign and send)
     * - Parameters:
     *   - privKeyHex: Private key (HEX format)
     *   - toAddress: Recipient address
     *   - amountSats: Transfer amount (satoshis)
     *   - feeSats: Fee (satoshis)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - fromAddress: Optional: Sender address (if not provided, defaults to Segwit address)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun oneClickTransfer(
        privKeyHex: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        isTestnet: Boolean = true,
        fromAddress: String? = null,
        onCompleted: ((Boolean, OneClickTransferResult?, String?) -> Unit)? = null
    ) {
        val params = mutableMapOf(
            "privKeyHex" to privKeyHex,
            "toAddress" to toAddress,
            "amountSats" to amountSats,
            "feeSats" to feeSats,
            "isTestnet" to isTestnet
        )
        
        fromAddress?.let {
            if (it.isNotEmpty()) {
                params["fromAddress"] = it
            }
        }
        
        bridge?.call("OneClickTransfer", params) { response ->
            if (showLog) {
                Log.d(TAG, "OneClickTransfer response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = OneClickTransferResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse transfer result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing OneClickTransfer response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * One-click transfer using suspend function (Coroutines)
     */
    suspend fun oneClickTransfer(
        privKeyHex: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        isTestnet: Boolean = true,
        fromAddress: String? = null
    ): Triple<Boolean, OneClickTransferResult?, String?> = suspendCancellableCoroutine { continuation ->
        oneClickTransfer(privKeyHex, toAddress, amountSats, feeSats, isTestnet, fromAddress) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }
    
    // MARK: estimateFee
    /**
     * Estimate fee
     * - Parameters:
     *   - inputsCount: Number of inputs (UTXO count), default 1
     *   - outputsCount: Number of outputs (recipient address + change address), default 2
     *   - isTestnet: Whether to use testnet, default true
     *   - addressType: Address type ('legacy' | 'segwit' | 'taproot'), default 'segwit'
     *   - n: Multisig threshold (for multisig only), default 1
     *   - m: Multisig total signers (for multisig only), default 1
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun estimateFee(
        inputsCount: Int = 1,
        outputsCount: Int = 2,
        isTestnet: Boolean = true,
        addressType: String = "segwit",
        n: Int = 1,
        m: Int = 1,
        onCompleted: ((Boolean, FeeEstimateResult?, String?) -> Unit)? = null
    ) {
        val params = mutableMapOf(
            "inputsCount" to inputsCount,
            "outputsCount" to outputsCount,
            "isTestnet" to isTestnet,
            "addressType" to addressType
        )
        
        // If multisig, add n and m parameters
        if (addressType == "multisig") {
            params["n"] = n
            params["m"] = m
        }
        
        bridge?.call("EstimateFee", params) { response ->
            if (showLog) {
                Log.d(TAG, "EstimateFee response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = FeeEstimateResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse fee estimate result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing EstimateFee response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Estimate fee using suspend function (Coroutines)
     */
    suspend fun estimateFee(
        inputsCount: Int = 1,
        outputsCount: Int = 2,
        isTestnet: Boolean = true,
        addressType: String = "segwit",
        n: Int = 1,
        m: Int = 1
    ): Triple<Boolean, FeeEstimateResult?, String?> = suspendCancellableCoroutine { continuation ->
        estimateFee(inputsCount, outputsCount, isTestnet, addressType, n, m) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: queryUTXO
    /**
     * Query UTXO list for an address
     * - Parameters:
     *   - address: Bitcoin address
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, utxos, errorMessage)
     *                  utxos: List of UTXO maps, each containing txHash, index, value
     */
    fun queryUTXO(
        address: String,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, List<Map<String, Any>>?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "address" to address,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("QueryUTXO", params) { response ->
            if (showLog) {
                Log.d(TAG, "QueryUTXO response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val dataArray = json.getJSONArray("data")
                val utxos = mutableListOf<Map<String, Any>>()
                
                for (i in 0 until dataArray.length()) {
                    val utxoJson = dataArray.getJSONObject(i)
                    val utxoMap = mutableMapOf<String, Any>()
                    utxoMap["txHash"] = utxoJson.getString("txHash")
                    utxoMap["index"] = utxoJson.getInt("index")
                    utxoMap["value"] = utxoJson.getLong("value")
                    utxos.add(utxoMap)
                }
                
                onCompleted?.invoke(true, utxos, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing QueryUTXO response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Query UTXO using suspend function (Coroutines)
     */
    suspend fun queryUTXO(
        address: String,
        isTestnet: Boolean = true
    ): Triple<Boolean, List<Map<String, Any>>?, String?> = suspendCancellableCoroutine { continuation ->
        queryUTXO(address, isTestnet) { success, utxos, error ->
            continuation.resume(Triple(success, utxos, error))
        }
    }

    // MARK: batchTransfer
    /**
     * Batch transfer: Create and sign batch transfer transaction (online method, automatically queries UTXO)
     * - Parameters:
     *   - outputs: Recipient list [{ address: String, value: Long }]
     *   - feeSats: Total fee (satoshis)
     *   - privKeyHex: Private key (HEX format)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - fromAddress: Optional: Sender address (if not provided, will derive Segwit address from private key)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun batchTransfer(
        outputs: List<Map<String, Any>>,
        feeSats: Long,
        privKeyHex: String,
        isTestnet: Boolean = true,
        fromAddress: String? = null,
        onCompleted: ((Boolean, BatchTransferResult?, String?) -> Unit)? = null
    ) {
        val params = mutableMapOf(
            "outputs" to outputs,
            "feeSats" to feeSats,
            "privKeyHex" to privKeyHex,
            "isTestnet" to isTestnet
        )
        
        fromAddress?.let {
            if (it.isNotEmpty()) {
                params["fromAddress"] = it
            }
        }
        
        bridge?.call("BatchTransfer", params) { response ->
            if (showLog) {
                Log.d(TAG, "BatchTransfer response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = BatchTransferResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse batch transfer result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing BatchTransfer response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Batch transfer using suspend function (Coroutines)
     */
    suspend fun batchTransfer(
        outputs: List<Map<String, Any>>,
        feeSats: Long,
        privKeyHex: String,
        isTestnet: Boolean = true,
        fromAddress: String? = null
    ): Triple<Boolean, BatchTransferResult?, String?> = suspendCancellableCoroutine { continuation ->
        batchTransfer(outputs, feeSats, privKeyHex, isTestnet, fromAddress) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: generateHtlcAddress
    /**
     * Generate HTLC address (single signature, non-Taproot)
     * - Parameters:
     *   - pubkey: Public key (HEX format, 66-character compressed public key)
     *   - lockHeight: Lock height
     *   - secretHex: Secret preimage (HEX format)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun generateHtlcAddress(
        pubkey: String,
        lockHeight: Int,
        secretHex: String,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, HTLCAddressResult?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "pubkey" to pubkey,
            "lockHeight" to lockHeight,
            "secretHex" to secretHex,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("GenerateHTLCAddress", params) { response ->
            if (showLog) {
                Log.d(TAG, "GenerateHTLCAddress response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = HTLCAddressResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse HTLC address result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing GenerateHTLCAddress response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Generate HTLC address using suspend function (Coroutines)
     */
    suspend fun generateHtlcAddress(
        pubkey: String,
        lockHeight: Int,
        secretHex: String,
        isTestnet: Boolean = true
    ): Triple<Boolean, HTLCAddressResult?, String?> = suspendCancellableCoroutine { continuation ->
        generateHtlcAddress(pubkey, lockHeight, secretHex, isTestnet) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: generateNoSigScriptAddress
    /**
     * Generate no-signature script address (TimeLock + HashLock, no signature required)
     * - Parameters:
     *   - lockHeight: Lock height
     *   - secretHex: Secret preimage (HEX format)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     * - Note: No-signature script means anyone who knows the preimage can spend the funds. Please ensure the preimage remains secret until unlock conditions are met.
     */
    fun generateNoSigScriptAddress(
        lockHeight: Int,
        secretHex: String,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, HTLCAddressResult?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "lockHeight" to lockHeight,
            "secretHex" to secretHex,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("GenerateNoSigScriptAddress", params) { response ->
            if (showLog) {
                Log.d(TAG, "GenerateNoSigScriptAddress response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = HTLCAddressResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse no-sig script address result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing GenerateNoSigScriptAddress response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Generate no-signature script address using suspend function (Coroutines)
     */
    suspend fun generateNoSigScriptAddress(
        lockHeight: Int,
        secretHex: String,
        isTestnet: Boolean = true
    ): Triple<Boolean, HTLCAddressResult?, String?> = suspendCancellableCoroutine { continuation ->
        generateNoSigScriptAddress(lockHeight, secretHex, isTestnet) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: generateMultisigAddress
    /**
     * Generate multisig address (non-Taproot address, generates P2SH and P2WSH addresses)
     * - Parameters:
     *   - threshold: Threshold number (Threshold N)
     *   - pubkeys: Public key list (array)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun generateMultisigAddress(
        threshold: Int,
        pubkeys: List<String>,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, MultisigAddressResult?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "threshold" to threshold,
            "pubkeys" to pubkeys,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("GenerateMultisigAddress", params) { response ->
            if (showLog) {
                Log.d(TAG, "GenerateMultisigAddress response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = MultisigAddressResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse multisig address result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing GenerateMultisigAddress response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Generate multisig address using suspend function (Coroutines)
     */
    suspend fun generateMultisigAddress(
        threshold: Int,
        pubkeys: List<String>,
        isTestnet: Boolean = true
    ): Triple<Boolean, MultisigAddressResult?, String?> = suspendCancellableCoroutine { continuation ->
        generateMultisigAddress(threshold, pubkeys, isTestnet) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: sendMultisigTransaction
    /**
     * Execute multisig transfer (create, sign and broadcast)
     * - Parameters:
     *   - multisigAddress: Multisig address
     *   - toAddress: Recipient address
     *   - amountSats: Transfer amount (satoshis)
     *   - feeSats: Fee (satoshis)
     *   - allPubkeys: All participant public key array (order must be correct)
     *   - signPrivKeys: Private key array for signing (must meet threshold number)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun sendMultisigTransaction(
        multisigAddress: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        allPubkeys: List<String>,
        signPrivKeys: List<String>,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, MultisigTransferResult?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "multisigAddress" to multisigAddress,
            "toAddress" to toAddress,
            "amountSats" to amountSats,
            "feeSats" to feeSats,
            "allPubkeys" to allPubkeys,
            "signPrivKeys" to signPrivKeys,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("SendMultisigTransaction", params) { response ->
            if (showLog) {
                Log.d(TAG, "SendMultisigTransaction response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = MultisigTransferResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse multisig transfer result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing SendMultisigTransaction response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Execute multisig transfer using suspend function (Coroutines)
     */
    suspend fun sendMultisigTransaction(
        multisigAddress: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        allPubkeys: List<String>,
        signPrivKeys: List<String>,
        isTestnet: Boolean = true
    ): Triple<Boolean, MultisigTransferResult?, String?> = suspendCancellableCoroutine { continuation ->
        sendMultisigTransaction(multisigAddress, toAddress, amountSats, feeSats, allPubkeys, signPrivKeys, isTestnet) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: unlockHtlcAddress
    /**
     * Unlock HTLC address and transfer (single signature, non-Taproot)
     * - Parameters:
     *   - htlcAddress: HTLC source address (P2WSH)
     *   - toAddress: Recipient address
     *   - amountSats: Transfer amount (satoshis)
     *   - feeSats: Fee (satoshis)
     *   - privKeyHex: Private key (HEX format)
     *   - lockHeight: Lock height
     *   - secretHex: Secret preimage (HEX format)
     *   - redeemScript: Redeem script (HEX format)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     */
    fun unlockHtlcAddress(
        htlcAddress: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        privKeyHex: String,
        lockHeight: Int,
        secretHex: String,
        redeemScript: String,
        isTestnet: Boolean = true,
        onCompleted: ((Boolean, HTLCUnlockResult?, String?) -> Unit)? = null
    ) {
        val params = mapOf(
            "htlcAddress" to htlcAddress,
            "toAddress" to toAddress,
            "amountSats" to amountSats,
            "feeSats" to feeSats,
            "privKeyHex" to privKeyHex,
            "lockHeight" to lockHeight,
            "secretHex" to secretHex,
            "redeemScript" to redeemScript,
            "isTestnet" to isTestnet
        )
        
        bridge?.call("HTLCUnlock", params) { response ->
            if (showLog) {
                Log.d(TAG, "HTLCUnlock response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = HTLCUnlockResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse HTLC unlock result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing HTLCUnlock response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Unlock HTLC address using suspend function (Coroutines)
     */
    suspend fun unlockHtlcAddress(
        htlcAddress: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        privKeyHex: String,
        lockHeight: Int,
        secretHex: String,
        redeemScript: String,
        isTestnet: Boolean = true
    ): Triple<Boolean, HTLCUnlockResult?, String?> = suspendCancellableCoroutine { continuation ->
        unlockHtlcAddress(htlcAddress, toAddress, amountSats, feeSats, privKeyHex, lockHeight, secretHex, redeemScript, isTestnet) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    // MARK: unlockNoSigScriptAddress
    /**
     * Unlock no-signature script address and transfer (no signature required, only preimage needed)
     * - Parameters:
     *   - noSigAddress: No-signature script source address (P2WSH)
     *   - toAddress: Recipient address
     *   - amountSats: Transfer amount (satoshis)
     *   - feeSats: Fee (satoshis)
     *   - lockHeight: Lock height
     *   - secretHex: Secret preimage (HEX format)
     *   - redeemScript: Redeem script (HEX format)
     *   - isTestnet: Whether to use testnet (default: true)
     *   - changeAddress: Optional change address. If not provided, change will be sent to toAddress (not recommended)
     *   - onCompleted: Completion callback with parameters (success, result, errorMessage)
     * - Note: No-signature script unlock does not require private key or signature, only the preimage
     */
    fun unlockNoSigScriptAddress(
        noSigAddress: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        lockHeight: Int,
        secretHex: String,
        redeemScript: String,
        isTestnet: Boolean = true,
        changeAddress: String? = null,
        onCompleted: ((Boolean, HTLCUnlockResult?, String?) -> Unit)? = null
    ) {
        val params = mutableMapOf(
            "noSigAddress" to noSigAddress,
            "toAddress" to toAddress,
            "amountSats" to amountSats,
            "feeSats" to feeSats,
            "lockHeight" to lockHeight,
            "secretHex" to secretHex,
            "redeemScript" to redeemScript,
            "isTestnet" to isTestnet
        )
        
        // 如果提供了找零地址，添加到参数中
        changeAddress?.let {
            params["changeAddress"] = it
        }
        
        bridge?.call("NoSigScriptUnlock", params) { response ->
            if (showLog) {
                Log.d(TAG, "NoSigScriptUnlock response = $response")
            }
            
            try {
                val json: JSONObject = when (response) {
                    is JSONObject -> response
                    is Map<*, *> -> {
                        fun mapToJsonObject(map: Map<*, *>): JSONObject {
                            val jsonObj = JSONObject()
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Map<*, *> -> jsonObj.put(key.toString(), mapToJsonObject(value))
                                    is List<*> -> {
                                        val jsonArray = JSONArray()
                                        value.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                                                else -> jsonArray.put(item)
                                            }
                                        }
                                        jsonObj.put(key.toString(), jsonArray)
                                    }
                                    null -> jsonObj.put(key.toString(), JSONObject.NULL)
                                    else -> jsonObj.put(key.toString(), value)
                                }
                            }
                            return jsonObj
                        }
                        mapToJsonObject(response)
                    }
                    is String -> JSONObject(response)
                    else -> JSONObject(response.toString())
                }
                
                val success = json.getBoolean("success")
                
                if (!success) {
                    val error = json.optString("error", "Unknown error")
                    onCompleted?.invoke(false, null, error)
                    return@call
                }
                
                val data = json.getJSONObject("data")
                val result = HTLCUnlockResult.fromJson(data)
                
                if (result != null) {
                    onCompleted?.invoke(true, result, null)
                } else {
                    onCompleted?.invoke(false, null, "Failed to parse no-sig script unlock result")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing NoSigScriptUnlock response", e)
                onCompleted?.invoke(false, null, "Error: ${e.message}")
            }
        }
    }
    
    /**
     * Unlock no-signature script address using suspend function (Coroutines)
     */
    suspend fun unlockNoSigScriptAddress(
        noSigAddress: String,
        toAddress: String,
        amountSats: Long,
        feeSats: Long,
        lockHeight: Int,
        secretHex: String,
        redeemScript: String,
        isTestnet: Boolean = true,
        changeAddress: String? = null
    ): Triple<Boolean, HTLCUnlockResult?, String?> = suspendCancellableCoroutine { continuation ->
        unlockNoSigScriptAddress(noSigAddress, toAddress, amountSats, feeSats, lockHeight, secretHex, redeemScript, isTestnet, changeAddress) { success, result, error ->
            continuation.resume(Triple(success, result, error))
        }
    }

    /**
     * Map language parameter to bip39 library supported key names
     * - Parameter language: Frontend language identifier
     * - Returns: bip39 library supported language key name
     * 
     * Note: bip39 library (v3.1.0) actually supports:
     * - 'english', 'chinese_simplified', 'chinese_traditional', 'korean', 'japanese', 
     *   'french', 'italian', 'spanish', 'czech', 'portuguese'
     * 
     * So no mapping is needed - the frontend language identifiers match bip39 keys.
     * This function is kept for potential future compatibility needs.
     */
    private fun mapLanguageToBip39(language: String): String {
        // bip39 v3.1.0 uses 'chinese_simplified' and 'chinese_traditional' directly
        // No mapping needed - return language as-is
        if (showLog) {
            Log.d(TAG, "Language parameter: $language (no mapping needed for bip39 v3.1.0)")
        }
        return language
    }

    companion object {
        private const val TAG = "BitcoinV1"
    }
}
