package com.concordia.blockchain.model.udp

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.net.DatagramPacket
import java.security.PublicKey
import java.util.logging.Level

import com.concordia.blockchain.conf.PublicKeyHolder
import com.concordia.blockchain.model.node._
import com.concordia.blockchain.constants.{MessageTag, MessageType}
import com.concordia.blockchain.crypto.Crypto

class RequestHandler(val datagramPacket: DatagramPacket, val node: Node, val requestRegister: RequestRegister) extends Thread{
  val recvMessageBytes = new ByteArrayInputStream(datagramPacket.getData)
  val recvInputStream = new ObjectInputStream(recvMessageBytes)
  val inMessage = recvInputStream.readObject.asInstanceOf[Message]
  val senderHostName = datagramPacket.getAddress.getHostName
  var signature = inMessage.signature
  val host = datagramPacket.getAddress.getHostName
  val port = datagramPacket.getPort

  override def run(): Unit = {
    requestRegister.addToPendingList(this)
    processMessage
    requestRegister.markAsProcessed(this)
  }

  def processMessage:Unit = {
    if (!verifySignature) {
      node.getLogger.info("Ignoring the packet for " + inMessage.messageType + " " + inMessage.messageTag
        + " as signature could not be verified for " + host + " " + inMessage.role)
      return
    }
    node.getLogger.log(Level.INFO, "Verified signature for host: " + host)
    node.getLogger.info("Received a message of Type: " + inMessage.messageType + " and Tag: " + inMessage.messageTag)
    inMessage.messageType match {
      case MessageType.INFO => {
        node.processInfoMessage(inMessage, host, port)
      }
      case MessageType.REQUEST => {
        node.processRequestMessage(inMessage, host, port)
      }
      case _ => {
        node.getLogger.log(Level.WARNING, "Not a valid Message type: " + inMessage.messageType)
      }
    }
  }

  def verifySignature: Boolean = {
    if(inMessage.signature != null){
      node.getLogger.log(Level.INFO, "Verifying signature for host: " + host)
      var pubKey = PublicKeyHolder.getPublicKey(host, inMessage.role)
      if (pubKey == null){
        //ASK host to send PublicKey
        node.getLogger.log(Level.WARNING, "Asking for public key to host: " + host)
        val msg = new Message(node.getRole, MessageType.REQUEST, MessageTag.PUBLIC_KEY)
        val rspns = node.udpClient.sendMessage(msg, host, node.peerAddress((host, inMessage.role.id)), true)
        node.getLogger.info("Set public key for host: " + host + " and role: " + rspns.role)
        PublicKeyHolder.setPublicKey(host, rspns.role, rspns.message.asInstanceOf[PublicKey])
        pubKey = rspns.message.asInstanceOf[PublicKey]
        node.getLogger.log(Level.INFO, "Received public key from host: " + host)
      }
      Crypto.validateSignature(inMessage.message, pubKey , inMessage.signature)
    } else {
      node.getLogger.log(Level.WARNING, "Message without signature from host: " + host)
      true
    }
  }
}