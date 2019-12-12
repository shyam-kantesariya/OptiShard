package com.concordia.blockchain.conf

import java.io.{FileInputStream, ObjectInputStream}
import java.security.PublicKey

import com.concordia.blockchain.constants.Role.Role

import scala.collection.mutable.Map

object PublicKeyHolder{
  val publicKeys = Map[(String, Int), PublicKey]()

  def setPublicKey(ip:String, role:Role, key:PublicKey) = {
    publicKeys((ip, role.id)) = key
  }

  def readPublicKeys(ip:String):PublicKey = {
    var filepath:String = null
    // (3) read the PublicKey object
    val ois = new ObjectInputStream(new FileInputStream(filepath))
    val publicKey = ois.readObject.asInstanceOf[PublicKey]
    publicKey
  }

  def getPublicKey(ip:String, role: Role):PublicKey = {
    publicKeys.getOrElse((ip,role.id),null)
  }
}