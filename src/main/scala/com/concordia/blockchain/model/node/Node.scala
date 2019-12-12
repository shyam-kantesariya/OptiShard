package com.concordia.blockchain.model.node

import java.sql.ResultSet
import java.util.logging.Level

import com.concordia.blockchain.conf.CustomLogger
import com.concordia.blockchain.constants.MessageTag.MessageTag

import scala.collection.mutable.Map
import com.concordia.blockchain.constants._
import com.concordia.blockchain.model.database.MySql
import com.concordia.blockchain.model.udp.{Client, Message, Server}
import com.concordia.blockchain.constants.Role.Role
import com.concordia.blockchain.crypto.Crypto

abstract class Node(role: Role, val myPublicIp:String, port:Int) {
  private val keyPair = Crypto.generateKeys
  val publicKey = keyPair.getPublic
  val privateKey = keyPair.getPrivate
  val keyPath = Constants.KEY_FILE_PATH + System.getProperty("file.separator") + myPublicIp + "_" + role.id + ".ser"
  val logger = CustomLogger.getLogger(myPublicIp, port, role)
  val udpServer:Server = new Server(port, this)
  new Thread(udpServer).start
  val udpClient:Client = new Client(logger)
  val mySql:MySql = new MySql(logger)
  val peerAddress:Map[Tuple2[String,Int], Int] = Map[Tuple2[String,Int], Int]()
  val conError = mySql.openConnection

  /*if (conError != 0) {
    println("Error while opening connection to MySql. Error code: " + conError)
    System.exit(conError)
  }*/
  registerPublicDetails
  Crypto.writePubliKeyInFile(keyPath, publicKey)
  getPeerDetails
  //printPeerDetails

  //Define all abstract methods
  def processInfoMessage(message: Message, host:String, port:Int)
  def processRequestMessage(message: Message, host:String, port:Int)
  def sendResponse(message:Any, messageTag:MessageTag, host:String, port:Int)

  def getRole = role
  def getPublicIp:String = myPublicIp
  def getLogger = logger
  def registerPublicDetails = {
    val sql = Sql.getRegistrationStatement(myPublicIp, keyPath, role)
    mySql.executeQuery(SqlOperation.UPDATE,sql)
  }

  def getPeerDetails = {
    val sql = Sql.allMiners
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    if (rs.isInstanceOf[ResultSet]){
      var result = rs.asInstanceOf[ResultSet]
      var ipHost:String = null
      var ipPort:Int = 0
      var roleId:Int = 0
      while (result.next) {
        ipHost = result.getString(Sql.colIpAddr)
        ipPort = result.getInt(Sql.colPort)
        roleId = result.getInt(Sql.colRoleId)
        peerAddress((ipHost,roleId)) = ipPort
      }
    }
  }

  def printPeerDetails = {
    peerAddress.foreach(keyVal => {
      logger.log(Level.INFO, "key: " + keyVal._1 + " Value: " + keyVal._2)
    })
  }

  def shutDown = {
    udpServer.shutDown
  }
}