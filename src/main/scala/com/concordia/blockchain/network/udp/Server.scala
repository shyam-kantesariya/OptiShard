package com.concordia.blockchain.network.udp

import java.net.{DatagramPacket, DatagramSocket, Socket}

import com.concordia.blockchain.constants.Constants

class Server (val port: Int) extends Runnable{
  def this() = this(Constants.UDP_SERVER_PORT)
  val socket: DatagramSocket = new DatagramSocket(port)
  var message:String = null;
  def startServer(): Unit ={
    while (true){
      val buffer = new Array[Byte](Constants.UDP_PACKET_SIZE)
      val packet = new DatagramPacket(buffer, buffer.length)
      socket.receive(packet)
      message = new String(packet.getData)
      val ipAddress = packet.getAddress().toString
      println("Server received from " + ipAddress + ": " + message)
      // respond
      socket.send(packet)
    }
  }
  def run: Unit ={
    startServer
  }
}
