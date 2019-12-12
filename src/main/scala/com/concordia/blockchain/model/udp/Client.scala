package com.concordia.blockchain.model.udp

import java.net.{DatagramPacket, DatagramSocket, InetSocketAddress, SocketException}

import com.concordia.blockchain.constants.Constants._
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.security.PublicKey
import java.util.logging.{Level, Logger}

import com.concordia.blockchain.conf.PublicKeyHolder
import com.concordia.blockchain.constants.{MessageTag, MessageType}

class Client(logger:Logger) {
  def sendMessage(message: Message, receiverIp:String, receiverPort: Int, expectAck: Boolean = false): Message = {
    try {
      //Serialize the message and send it over UDP
      val socket = new DatagramSocket()
      val address = new InetSocketAddress(receiverIp, receiverPort)
      val outputStream = new ByteArrayOutputStream
      val os = new ObjectOutputStream(outputStream)
      os.writeObject(message)
      val buffer = outputStream.toByteArray
      logger.info("Sending packet to " + receiverIp + ":" + receiverPort)
      val packet = new DatagramPacket(buffer, buffer.length, address)
      socket.send(packet)
      logger.info("Sent packet to " + receiverIp + ":" + receiverPort)
      //Return null if there is no expectation of acknowledgement message

      if (!expectAck) return null

      //Receive the acknowledgement message and deserialize it to Message object
      var recvBuffer: Array[Byte] = new Array[Byte](UDP_PACKET_SIZE)
      val recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length)
      logger.info("Waiting for response packet from " + receiverIp + ":" + receiverPort)
      socket.receive(recvPacket)
      logger.info("Received response packet from " + receiverIp + ":" + receiverPort)
      val recvData = recvPacket.getData
      val recvMessageBytes = new ByteArrayInputStream(recvData)
      val recvInputStream = new ObjectInputStream(recvMessageBytes)
      val recvMessage = recvInputStream.readObject.asInstanceOf[Message]
      //****************************Need to eanble at one point***********************************
      //processResponse(recvMessage, recvPacket.getAddress.getHostName)
      return recvMessage
    } catch {
      case se: SocketException => {
        logger.log(Level.WARNING, "SocketException while receiving response: " + se.printStackTrace)
        return null
      }
      case e: Exception => {
        logger.log(Level.WARNING, "Exception while receiving response: " + e.printStackTrace)
        return null
      }
    }
  }
}