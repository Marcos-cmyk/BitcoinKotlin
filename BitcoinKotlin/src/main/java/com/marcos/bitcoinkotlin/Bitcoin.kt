package com.marcos.bitcoinkotlin

import android.content.Context
import android.graphics.Bitmap
import android.webkit.*
import java.lang.reflect.InvocationTargetException

public class Bitcoin(context: Context, _webView: WebView) {
    private val webView = _webView
    public var isSuccess: Boolean = false
    private var bridge = WebViewJavascriptBridge(_context = context,_webView = webView)
    var onCompleted = { _: Boolean  -> }
    private var showLog: Boolean = false
    init {
        setAllowUniversalAccessFromFileURLs(webView)
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE)
    }
    public fun setup(showLog: Boolean = true, onCompleted: (Boolean) -> Unit) {
        this.showLog = showLog
        this.onCompleted = onCompleted
        webView.webViewClient = webClient
        if (showLog) {
            bridge.consolePipe = object : ConsolePipe {
                override fun post(string : String){
                    println("Next line is javascript console.log->>>")
                    println(string)
                }
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
        val handler = object :Handler {
            override fun handler(map: HashMap<String, Any>?, callback: Callback) {
                isSuccess = true
                println("js load finished")
                onCompleted(true)
            }
        }
        bridge.register("generateBitcoin",handler)
    }

    public fun generateAccount(onCompleted: (Boolean,String, String,String,String) -> Unit) {
        val data = java.util.HashMap<String, Any>()
        bridge.call("createAccount", data, object : Callback {
            override fun call(map: HashMap<String, Any>?){
                if (showLog) {
                    println(map)
                }
                val state =  map!!["state"] as Boolean
                if (state) {
                    val address = map["address"] as String
                    val privateKey = map["privateKey"] as String
                    val mnemonic = map["mnemonic"] as String
                    onCompleted(state,address, privateKey,mnemonic, "")
                } else {
                    val error = map["error"] as String
                    onCompleted(state,"","","",error)
                }
            }
        })
    }

    public fun importAccountFromMnemonic(mnemonic: String,
                                         onCompleted: (Boolean,String,String,String) -> Unit) {
        val data = java.util.HashMap<String, Any>()
        data["mnemonic"] = mnemonic
        bridge.call("importAccountFromMnemonic", data, object : Callback {
            override fun call(map: HashMap<String, Any>?){
                if (showLog) {
                    println(map)
                }
                val state =  map!!["state"] as Boolean
                if (state) {
                    val address = map["address"] as String
                    val privateKey = map["privateKey"] as String
                    onCompleted(state,address,privateKey, "")
                } else {
                    val error = map["error"] as String
                    onCompleted(state,"","",error)
                }
            }
        })
    }

    public fun importAccountFromPrivateKey(privateKey: String,
                                           onCompleted: (Boolean,String,String) -> Unit) {
        val data = java.util.HashMap<String, Any>()
        data["privateKey"] = privateKey
        bridge.call("importAccountFromPrivateKey", data, object : Callback {
            override fun call(map: HashMap<String, Any>?){
                if (showLog) {
                    println(map)
                }
                val state =  map!!["state"] as Boolean
                if (state) {
                    val address = map["address"] as String
                    onCompleted(state,address, "")
                } else {
                    val error = map["error"] as String
                    onCompleted(state,"",error)
                }
            }
        })
    }
    public fun getBTCBalance(address: String,
                             onCompleted: (Boolean,String,String) -> Unit) {
        val data = java.util.HashMap<String, Any>()
        data["address"] = address
        bridge.call("getBTCBalance", data, object : Callback {
            override fun call(map: HashMap<String, Any>?){
                if (showLog) {
                    println(map)
                }
                val state =  map!!["state"] as Boolean
                if (state) {
                    val balance = map["balance"] as String
                    onCompleted(state,balance,"")
                } else {
                    val error = map["error"] as String
                    onCompleted(state,"",error)
                }
            }
        })
    }
    public fun transfer(privateKey: String,
                           outputs:MutableList<HashMap<String, String>>,
                           fee: Double,
                           onCompleted: (Boolean,String,String) -> Unit) {
        val data = java.util.HashMap<String, Any>()
        data["outputs"] = outputs
        data["fee"] = fee
        data["privateKey"] = privateKey
        bridge.call("BTCTransfer", data, object : Callback {
            override fun call(map: HashMap<String, Any>?){
                if (showLog) {
                    println(map)
                }
                val state =  map!!["state"] as Boolean
                if (state) {
                    val hash = map["hash"].toString()
                    onCompleted(state,hash,"")
                } else {
                    val error = map["error"] as String
                    onCompleted(state,"",error)
                }
            }
        })
    }
    public fun estimateBtcTransferFee(inputsCount: Int = 1,
                                      outputsCount: Int = 1,
                                         onCompleted: (Boolean,Double,Double,Double,String) -> Unit) {
        val data = java.util.HashMap<String, Any>()
        data["inputsCount"] = inputsCount
        data["outputsCount"] = outputsCount
        bridge.call("estimateFee", data, object : Callback {
            override fun call(map: HashMap<String, Any>?){
                if (showLog) {
                    println(map)
                }
                val state =  map!!["state"] as Boolean
                if (state) {
                    val high = map["high"] as Double
                    val medium = map["medium"] as Double
                    val low = map["low"] as Double
                    onCompleted(state,high,medium,low,"")
                } else {
                    val error = map["error"] as String
                    onCompleted(state,0.0,0.0,0.0,error)
                }
            }
        })
    }

    private val webClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            println("shouldOverrideUrlLoading")
            return false
        }
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            println("onPageStarted")
            bridge.injectJavascript()
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            println("onPageFinished")
        }
    }
    //Allow Cross Domain
    private fun setAllowUniversalAccessFromFileURLs(webView: WebView) {
        try {
            val clazz: Class<*> = webView.settings.javaClass
            val method = clazz.getMethod(
                "setAllowUniversalAccessFromFileURLs", Boolean::class.javaPrimitiveType
            )
            method.invoke(webView.settings, true)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }
}

