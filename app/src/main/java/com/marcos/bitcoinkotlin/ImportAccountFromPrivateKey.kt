package com.marcos.bitcoinkotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
//import com.smithsophiav.web3kotlin.ETHWeb

class ImportAccountFromPrivateKey: AppCompatActivity() {
    private var title: TextView? = null
    private var password: TextView? = null
    private var privateKeyEditText: EditText? = null
    private var walletDetailEditText: EditText? = null
    private var importAccountFromPrivateKeyBtn: Button? = null
    private var mWebView: WebView? = null
//    private var web3: ETHWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_account_from_privatekey)
        setupContent()
    }
    private fun setupContent() {
        title = findViewById(R.id.title)
        password = findViewById(R.id.password)
        privateKeyEditText = findViewById(R.id.privateKey)
        walletDetailEditText = findViewById(R.id.wallet_detail)
        importAccountFromPrivateKeyBtn = findViewById(R.id.btn_import_account_from_privateKey)
        mWebView =  findViewById(R.id.webView)
//        web3 = ETHWeb(this, _webView = mWebView!!)
        importAccountFromPrivateKeyBtn?.setOnClickListener{
//            importAccountFromPrivateKey()
        }
    }
//    private fun importAccountFromPrivateKey() {
//        val onCompleted = {result : Boolean ->
//            println("ETHWeb setup Completed------->>>>>")
//            println(result)
//            importAccountFromPrivateKeyAction()
//        }
//        if (web3?.isWeb3LoadFinished == false) {
//            web3?.setup(true,onCompleted = onCompleted)
//        }  else  {
//            importAccountFromPrivateKeyAction()
//        }
//    }

//    @SuppressLint("SetTextI18n")
//    private fun importAccountFromPrivateKeyAction() {
//        val password = password?.text.toString()
//        val privateKey = privateKeyEditText?.getText().toString();
//        if (password.isNotEmpty()&&privateKey.isNotEmpty()) {
//            val onCompleted = {state : Boolean, address: String,keystore: String,error: String ->
//                this.runOnUiThread {
//                    if (state) {
//                        val text =
//                            "address: " + address + "\n\n" +
//                            "keystore: " + keystore
//                        walletDetailEditText?.setText(text)
//                    } else {
//                        walletDetailEditText?.setText(error)
//                    }
//                }
//            }
//            walletDetailEditText?.setText("Import Accounting.......")
//            web3?.importAccountFromPrivateKey(privateKey,password,onCompleted = onCompleted)
//
//        }
//    }
}