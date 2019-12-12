package com.concordia.blockchain.model.node

import java.security.PublicKey
import java.util.logging.Level

import com.concordia.blockchain.conf.PublicKeyHolder
import com.concordia.blockchain.constants.MessageTag.MessageTag
import com.concordia.blockchain.model.udp.Message
import com.concordia.blockchain.constants.{Constants, MessageTag, MessageType, Role}
import com.concordia.blockchain.constants.Role.Role
import com.concordia.blockchain.crypto.Crypto

import scala.collection.mutable.{ListBuffer, Map}

//Ref: https://www.scala-lang.org/old/node/125
class WorkerLeader(role: Role, myPublicIp:String, port:Int) extends Node(role, myPublicIp, port) with Runnable{
  def this(role: Role) = this(role: Role, Constants.UDP_LOCAL_HOST, Constants.UDP_SERVER_PORT)

  private var txListToBeProcessed = List[Int]()
  private var team:List[String] = List[String]()
  private var processedTxDigestHostList:Tuple2[Int,List[String]] = null
  private val txDigestList = ListBuffer[(Int, String)]()
  private var processedTxList = List[Int]()
  private var finalAggTxDigest:Map[Int,Int] = Map[Int,Int]()
  private var manager:Manager = null
  private var finalAggProcessedTxDigest:Map[Int,Int] = Map[Int,Int]()
  private var partnerAcrossCommittees:List[List[String]] = null
  private var committeeId:Int = -1

  override def run(): Unit = {
    logger.info("WorkerLeader started")
    while (team.isEmpty){
      logger.info("Waiting for team details")
      Thread.sleep(1234)
    }
    registerLeaderToFollowers
    sendPartnerDetails
    sendTxListToBeProcessed
    setProcessedTxDigest
    //askProcessedTxList
    sendProcessedTxDigestHostListToManager
    while (finalAggProcessedTxDigest.isEmpty){
      logger.info("Waiting for final agg result of processed Tx from leader")
      Thread.sleep(1234)
    }
    sendFinalAggProcessedTxDigestToWorkers
    shutDown
    logger.info("WorkerLeader completed")
  }

  def setManager(manager:Manager)={
    logger.info("Set manager")
    this.manager = manager
  }

  def setTeam(members:List[String]):Unit={
    logger.info("Set team members: " + members.mkString(","))
    team = members
  }

  def setPartnerDetails(partners:List[List[String]]) = {
    logger.info("Set partner details across committees")
    partnerAcrossCommittees = partners
  }

  def setLeaderOfCommittee(cmtId:Int) = {
    logger.info("Set committee leader of committee id: " + cmtId)
    committeeId = cmtId
  }

  def registerLeaderToFollowers = {
    logger.info("Informing followers about its leader")
    val msg = new Message(role, MessageType.INFO, MessageTag.COMMITTEE_LEADER)
    team.foreach(worker => {
      udpClient.sendMessage(msg, worker, peerAddress.get((worker, Role.NON_CORE_FOLLOWER.id)).get)
    })
    logger.info("Informed followers about its leader")
  }

  def sendPartnerDetails = {
    logger.info("Sending partner details across committees to workers")
    val sign = Crypto.generateSignature(partnerAcrossCommittees, privateKey)
    val msg = new Message(role, MessageType.INFO, MessageTag.PARTNERS_ACROSS_COMMITTEES, partnerAcrossCommittees, sign)
    team.foreach(worker => {
      udpClient.sendMessage(msg, worker, peerAddress.get((worker, Role.NON_CORE_FOLLOWER.id)).get)
    })
    logger.info("Sent partner details across committees to workers")
  }

  def getTransactionsToProcess: List[Int] = {
    txListToBeProcessed
  }

  def setTxListToBeProcessed(list:List[Int]) = {
    logger.info("Set Tx list to be processed")
    txListToBeProcessed = list.sorted
  }

  def sendTxListToBeProcessed = {
    logger.info("Send Tx list to be processed to workers")
    val sign = Crypto.generateSignature((committeeId, txListToBeProcessed), privateKey)
    val msg = Message(role, MessageType.INFO, MessageTag.TX_LIST_TO_BE_PROCESSED, (committeeId, txListToBeProcessed), sign)
    team.foreach(worker => {
      udpClient.sendMessage(msg, worker, peerAddress.get((worker, Role.NON_CORE_FOLLOWER.id)).get)
    })
  }

  def getTxListToBeProcessed:List[Int] = txListToBeProcessed

  /*def requestProcessedTxDigest(ipAdd:String):Unit={
    val message = new Message(MessageType.REQUEST, MessageTag.PROCESSED_TX_DIGEST)
    val responseMessage = udpClient.sendMessage(message, true)
    val digest = responseMessage.message.asInstanceOf[Int]
    txDigest(digest) = (txDigest.getOrElse(digest,0) + 1)
  }*/

  def setProcessedTxDigest:Unit={
    logger.info("Setting processed Tx digest based on majority")
    var maxDigestCnt = 0
    var maxDigestVal = 0
    var digestHostMap:Map[Int, ListBuffer[String]] = Map[Int, ListBuffer[String]]()
    var receivedMajority = false
    while (!receivedMajority){
      txDigestList.foreach(digestHost => {
        val list = digestHostMap.getOrElse(digestHost._1, new ListBuffer[String])
        list += digestHost._2
        digestHostMap(digestHost._1) = list
        txDigestList -= digestHost
      })
      digestHostMap.foreach(keyVal => {
        if (keyVal._2.length > maxDigestCnt) {
          maxDigestCnt = keyVal._2.length
          maxDigestVal = keyVal._1
        }
      })
      if (maxDigestCnt >= Constants.CONSENSUS_MAJORITY){
        receivedMajority = true
        processedTxDigestHostList = (maxDigestVal, digestHostMap.get(maxDigestVal).get.toList)
        logger.info("Set processed Tx digest value: " + processedTxDigestHostList._1 + " with majority of "
          + processedTxDigestHostList._2.length)
      } else {
        Thread.sleep(1234)
      }
    }
  }

  def askProcessedTxList = {
    logger.info("Ask processed Tx list to worker")
    val msg = new Message(role, MessageType.REQUEST, MessageTag.PROCESSED_TX_LIST)
    val response = udpClient.sendMessage(msg, processedTxDigestHostList._2(0),
      peerAddress.get(processedTxDigestHostList._2(0), Role.NON_CORE_FOLLOWER.id).get, true)
    logger.info("Received Tx list in response: " + response.getClass)
    processedTxList = response.message.asInstanceOf[List[Int]]
  }

  def requestProcessedTxDigest(workerHostName:String) = {
    logger.info("Requesting processed Tx digest to worker: " + workerHostName)
    val msg = new Message(role, MessageType.REQUEST, MessageTag.PROCESSED_TX_DIGEST)
    udpClient.sendMessage(msg, workerHostName, peerAddress.get((workerHostName, Role.NON_CORE_FOLLOWER.id)).get,
      true)
  }

  def getProcessedTxDigest:Int={
    logger.info("Get processed Tx digest")
    while(processedTxDigestHostList==null) {
      Thread.sleep(1234)
    }
    processedTxDigestHostList._1
  }

  def onReceiveOfProcessedTxDigest(hostName:String, digest:Int) = {
    logger.info("Received processed Tx digest: " + digest + " from worker: " + hostName)
    val digestHost = (digest, hostName)
    txDigestList += digestHost
  }

  def sendProcessedTxDigestHostListToManager = {
    logger.info("Sending processed Tx digest and host list to Manager")
    manager.setProcessedTxDigestHostList(processedTxDigestHostList)
    logger.info("Sent processed Tx digest and host list to Manager")
  }


  def setFinalAggProcessedTxDigest(aggMap:Map[Int, Int]) = {
    logger.info("Set final agg processed Tx digest")
    finalAggProcessedTxDigest = aggMap
  }

  def sendFinalAggProcessedTxDigestToWorkers = {
    logger.info("Sending final agg processed Tx digest to workers")
    val sign = Crypto.generateSignature(finalAggProcessedTxDigest, privateKey)
    val msg = new Message(role, MessageType.INFO, MessageTag.FINAL_AGG_PROCESSED_TX_DIGEST, finalAggProcessedTxDigest, sign)
    team.foreach(worker => {
      udpClient.sendMessage(msg, worker, peerAddress((worker, Role.NON_CORE_FOLLOWER.id)))
    })
    logger.info("Sent final agg processed Tx digest to workers")
  }

  override def processInfoMessage(message: Message, host: String, port: Int): Unit = {
    message.messageTag match {
      case MessageTag.PROCESSED_TX_DIGEST => {
        logger.info("Received an information message of Tag: " + MessageTag.PROCESSED_TX_DIGEST)
        val digest = message.message.asInstanceOf[Int]
        onReceiveOfProcessedTxDigest(host, digest)
      }
      case MessageTag.PUBLIC_KEY => {
        logger.info("Received an information message of Tag:  " + MessageTag.PUBLIC_KEY)
        PublicKeyHolder.setPublicKey(host, message.role, message.message.asInstanceOf[PublicKey])
      }
    }
  }

  override def processRequestMessage(message: Message, host: String, port: Int): Unit = {
    var response:Any = null
    var responseFlg:Boolean = true
    message.messageTag match {
      case MessageTag.PROCESSED_TX_DIGEST => {
        logger.info("Received a request of Tag: " + MessageTag.PROCESSED_TX_DIGEST)
        val digest = getProcessedTxDigest
        response = digest
      }
      case MessageTag.TX_LIST_TO_BE_PROCESSED => {
        logger.info("Received a request of Tag: " + MessageTag.TX_LIST_TO_BE_PROCESSED)
        val txList = getTransactionsToProcess
        response = txList
      }
      case MessageTag.PUBLIC_KEY => {
        logger.info("Received a request of Tag: " + MessageTag.PUBLIC_KEY)
        response = publicKey
      }
      case MessageTag.PING => {
        logger.info("Received a request of Tag: " + MessageTag.PING)
        response = "HI"
      }
      case _ => {
        logger.log(Level.WARNING, "Ignoring a request of invalid tag: " + message.messageTag)
        responseFlg = false
      }
    }
    if(responseFlg)
      sendResponse(response, message.messageTag, host, port)
  }

  override def sendResponse(message: Any, messageTag: MessageTag, host: String, port: Int): Unit = {
    val sign = Crypto.generateSignature(message, privateKey)
    val responseMessage = new Message(role, MessageType.RESPONSE, messageTag, message, sign)
    logger.info("Sending response for tag: " + messageTag)
    udpClient.sendMessage(responseMessage, host, port)
    logger.info("Sent response for tag: " + messageTag)
  }

  def reset:Unit = {
    logger.info("Reset everything for the second round")
    txListToBeProcessed = List[Int]()
    team = List[String]()
    processedTxDigestHostList = null
    //txDigest.empty
  }
}