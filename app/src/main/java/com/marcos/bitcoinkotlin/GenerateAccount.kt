package com.marcos.bitcoinkotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
//import com.smithsophiav.web3kotlin.ETHWeb

class GenerateAccount: AppCompatActivity() {

    private var title: TextView? = null
    private var password: TextView? = null
    private var walletDetail: EditText? = null
    private var generateAccountBtn: Button? = null
    private var mWebView: WebView? = null
//    private var web3: ETHWeb? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.generate_account)
        setupContent()
    }

    private fun setupContent() {
        title = findViewById(R.id.title)
        password = findViewById(R.id.password)
        walletDetail = findViewById(R.id.wallet_detail)
        generateAccountBtn = findViewById(R.id.btn_generate_account)
        mWebView =  findViewById(R.id.webView)
//        web3 = ETHWeb(this, _webView = mWebView!!)
        generateAccountBtn?.setOnClickListener{
            generateAccount()
        }
    }
    private fun generateAccount() {
//        val onCompleted = {result : Boolean ->
//            println("ETHWeb setup Completed------->>>>>")
//            println(result)
//            generateAccountAction()
//        }
//        if (web3?.isWeb3LoadFinished == false) {
//            web3?.setup(true,onCompleted = onCompleted)
//        }  else  {
//            generateAccountAction()
//        }
    }
//    @SuppressLint("SetTextI18n")
//    private fun generateAccountAction() {
//        val password = password?.text.toString()
//        if (password.isNotEmpty()) {
//            val onCompleted = {state : Boolean,address:String, mnemonic:String, privateKey:String, keystore:String,error: String ->
//                this.runOnUiThread {
//                    if (state) {
//                        val text =
//                        "address: " + address + "\n\n" +
//                        "mnemonic: " + mnemonic + "\n\n" +
//                        "privateKey: " + privateKey + "\n\n" +
//                        "keystore: " + keystore
//                        walletDetail?.setText(text)
//                    } else {
//                        walletDetail?.setText(error)
//                    }
//                }
//            }
//            walletDetail?.setText("generate Accounting.......")
//            web3?.generateAccount(password,onCompleted = onCompleted)
//        }
//    }
}