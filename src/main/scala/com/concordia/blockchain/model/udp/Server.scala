package com.concordia.blockchain.model.udp

import java.net.{DatagramPacket, DatagramSocket}
import java.util.logging.Level

import com.concordia.blockchain.model.node.Node
import com.concordia.blockchain.constants.Constants

class Server (val port: Int, val node:Node) extends Runnable{
  node.logger.log(Level.INFO, "Starting UDP server on port " + port)
  val socket: DatagramSocket = new DatagramSocket(port)
  var message:String = null;
  var shutDownFlg = false
  val requestRegister = new RequestRegister
  def startServer(): Unit ={
    while (!shutDownFlg){
      val buffer = new Array[Byte](Constants.UDP_PACKET_SIZE)
      val packet = new DatagramPacket(buffer, buffer.length)
      //println("Server will receive Packet on port " + packet.getPort)
      socket.receive(packet)
      node.getLogger.log(Level.INFO ,"Server has received the packet from port: " + packet.getPort)
      val req = new RequestHandler(packet, node, requestRegister)
      req.start
      //socket.send(packet)
    }
  }

  def run ={
    node.getLogger.log(Level.INFO, "Staring UDP Server")
    startServer
  }

  def shutDown = {
    shutDownFlg = true
    if (requestRegister.isAnyRequestPending){
      node.getLogger.info("Waiting for " + requestRegister.getPendingRequestCnt +" pending requests")
    }
    node.getLogger.info("Shutting down UDP server as pending request count is: " + requestRegister.getPendingRequestCnt +" now")
  }
}