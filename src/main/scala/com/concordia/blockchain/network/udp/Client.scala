package com.concordia.blockchain.network.udp

import java.net.{DatagramPacket, DatagramSocket, InetSocketAddress, SocketException}

import com.concordia.blockchain.constants.Constants._
import com.concordia.blockchain.network.Message
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

class Client(val host:String, val port:Int) {
  def this(port:Int) = this(UDP_LOCAL_HOST, port)
  def this(host:String) = this(host, UDP_SERVER_PORT)
  def this() = this(UDP_LOCAL_HOST, UDP_SERVER_PORT)
  def sendMessage(message:Message, expectAck:Boolean=false): Message ={
    try{
      //Serialize the message and send it over UDP
      val socket = new DatagramSocket()
      val address = new InetSocketAddress(host, port)
      val outputStream = new ByteArrayOutputStream
      val os = new ObjectOutputStream(outputStream)
      os.writeObject(message)
      val buffer = outputStream.toByteArray
      var packet = new DatagramPacket(buffer, buffer.length, address)
      socket.send(packet)

      //Return null if there is no expectation of acknowledgement message
      if(!expectAck) return null

      //Receive the acknowledgement message and deserialize it to Message object
      var recvBuffer:Array[Byte] = new Array[Byte](UDP_PACKET_SIZE)
      packet = new DatagramPacket(recvBuffer, recvBuffer.length)
      socket.receive(packet)
      val recvData = packet.getData
      val recvMessageBytes = new ByteArrayInputStream(recvData)
      val recvInputStream = new ObjectInputStream(recvMessageBytes)
      val recvMessage = recvInputStream.readObject.asInstanceOf[Message]
      return recvMessage
    } catch {
      case se:SocketException => {
        println(se.getMessage)
        return null
      }
      case e:Exception => {
        println(e.getMessage)
        return null
      }
    }
  }
}