# BitcoinKotlin
**BitcoinKotlin** is an Android toolbelt for interaction with the BTC network.

![language](https://img.shields.io/badge/Language-Kotlin-green)
![jitpack](https://img.shields.io/badge/support-jitpack-green)
![jitpack](https://img.shields.io/badge/support-sepolia-green)


![](Resource/Demo01.png)

For more specific usage, please refer to the [demo](https://github.com/Marcos-cmyk/BitcoinKotlin/tree/main/app)

## JitPack.io

I strongly recommend https://jitpack.io
```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.Marcos-cmyk:BitcoinKotlin:1.0.0'
}
```

##### Setup BitcoinKotlin 
```kotlin
val onCompleted = {result : Boolean ->
   //Do Something...
}
if (bitcoin?.isSuccess == false) {
    bitcoin?.setup(true,onCompleted = onCompleted)
}  else  {
    //Do Something...
}
```
##### Generate Account
```Kotlin
val onCompleted = {state : Boolean,address:String,privateKey:String,mnemonic:String,error: String ->
this.runOnUiThread {
    if (state) {
        val text =
        "address: " + address + "\n\n" +
        "mnemonic: " + mnemonic + "\n\n" +
        "privateKey: " + privateKey
        walletDetail?.setText(text)
    } else {
        walletDetail?.setText(error)
    }
}
}
walletDetail?.setText("generate Accounting.......")
bitcoin?.generateAccount(onCompleted = onCompleted)
```

##### Import Account From PrivateKey
```Kotlin
val privateKey = privateKeyEditText?.getText().toString();
if (privateKey.isNotEmpty()) {
    val onCompleted = {state : Boolean, address: String,error: String ->
        this.runOnUiThread {
            if (state) {
                val text = "address: " + address
                walletDetailEditText?.setText(text)
            } else {
                walletDetailEditText?.setText(error)
            }
        }
    }
    walletDetailEditText?.setText("Import Accounting.......")
    bitcoin?.importAccountFromPrivateKey(privateKey,onCompleted = onCompleted)
}
```
##### Import Account From Mnemonic
```Kotlin
val mnemonic = mnemonicEditText?.getText().toString();
if (mnemonic.isNotEmpty()) {
    val onCompleted = {state : Boolean, address: String,privateKey: String,error: String ->
        this.runOnUiThread {
            if (state) {
                val text =
                    "address: " + address + "\n\n" +
                    "privateKey: " + privateKey
                walletDetailEditText?.setText(text)
            } else {
                walletDetailEditText?.setText(error)
            }
        }
    }
    walletDetailEditText?.setText("Import Accounting.......")
    bitcoin?.importAccountFromMnemonic(mnemonic,onCompleted = onCompleted)
}
```
##### Estimate Fee for BTC Transfer
```Kotlin
val onCompleted = {state : Boolean,
                           high: Double,
                           medium:Double,
                           low:Double,
                           error:String ->
this.runOnUiThread {
    println("Estimate fee finised.")
    if (state){
        val highFormatted = String.format("%.2f", high)
        val mediumFormatted = String.format("%.2f", medium)
        val lowFormatted = String.format("%.2f", low)
        val text = "Send BTC have three estimated fee. \n high: $highFormatted Satoshis. \n medium: $mediumFormatted Satoshis. \n low: $lowFormatted Satoshis"
        feeDetailEditText?.setText(text)
    } else {
        feeDetailEditText?.setText(error)
    }
  }
}
bitcoin?.estimateBtcTransferFee(1,2,onCompleted = onCompleted)
println("Estimate fee start.")
```

##### BTC Transfer
```Kotlin
val privateKey = privateKeyEditText?.text.toString()
val toAddress = receiveEditText?.text.toString()
val amount = amountEditText?.text.toString()

val outputs: MutableList<HashMap<String, String>> = mutableListOf(
    hashMapOf("address" to toAddress, "amount" to "0.00001")
)
outputs.add(hashMapOf("address" to "secondAddress", "amount" to "0.00001"))

println("Support a Bitcoin address sending to multiple Bitcoin addresses simultaneously.")

if (toAddress.isNotEmpty() && amount.isNotEmpty() && privateKey.isNotEmpty()) {
    val onCompleted = {state : Boolean, hash: String,error:String ->
        println("btcTransfer Finished.")
        this.runOnUiThread {
            if (state){
                hashValue?.text = hash
            } else {
                hashValue?.text = error
            }
        }
    }
    val fee = 2000.0
    bitcoin?.transfer(privateKey,outputs,fee,onCompleted = onCompleted)
    println("btcTransfer start.")
}
```
## License

BitcoinKotlin is released under the MIT license. [See LICENSE](https://github.com/Marcos-cmyk/BitcoinKotlin/blob/master/LICENSE) for details.
