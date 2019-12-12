package com.concordia.blockchain.crypto

import java.io._
import java.security._
import java.util.Date
import java.sql.Timestamp

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec

//Ref: https://github.com/anonrig/bouncycastle-implementations/blob/master/ecdsa.java

object Crypto {
  Security.addProvider(new BouncyCastleProvider)

  def generateSignature(hashVal: Any, privateKey: PrivateKey): Array[Byte] = {
    val ecdsaSign = Signature.getInstance("SHA256withECDSA", "BC")
    ecdsaSign.initSign(privateKey)
    ecdsaSign.update(hashVal.hashCode.toByte)
    val signature = ecdsaSign.sign
    signature
  }

  def validateSignature(hashVal: Any, publicKey: PublicKey, signature: Array[Byte]): Boolean = {
    val ecdsaVerify = Signature.getInstance("SHA256withECDSA", "BC")
    ecdsaVerify.initVerify(publicKey)
    ecdsaVerify.update(hashVal.hashCode.toByte)
    ecdsaVerify.verify(signature)
  }

  def generateKeys: KeyPair = {
    //	Other named curves can be found in http://www.bouncycastle.org/wiki/display/JA1/Supported+Curves+%28ECDSA+and+ECGOST%29
    val ecSpec = ECNamedCurveTable.getParameterSpec("B-571")
    val g = KeyPairGenerator.getInstance("ECDSA", "BC")
    g.initialize(ecSpec, new SecureRandom)
    g.generateKeyPair
  }

  def writePubliKeyInFile(filepath:String, publicKey: PublicKey):Unit={
    // (2) write the instance out to a file
    val oos = new ObjectOutputStream(new FileOutputStream(filepath))
    oos.writeObject(publicKey)
    oos.close
  }

  //Serialize an Object
  //https://alvinalexander.com/scala/how-to-use-serialization-in-scala-serializable-trait
}