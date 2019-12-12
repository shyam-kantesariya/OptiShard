package com.concordia.blockchain.model.node

import java.security.PublicKey
import java.sql.ResultSet
import java.util.logging.Level

import com.concordia.blockchain.conf.PublicKeyHolder
import com.concordia.blockchain.constants.MessageTag.MessageTag
import com.concordia.blockchain.model.udp.Message
import com.concordia.blockchain.constants.Role.Role
import com.concordia.blockchain.constants._
import com.concordia.blockchain.crypto.Crypto

import scala.collection.mutable.{ListBuffer, Map}
import scala.util.Random
import scala.math.{floor}

class ManagerLeader(role: Role, myPublicIp: String, port: Int)
  extends Node(role, myPublicIp, port) with Runnable {
  def this(role: Role) = this(role: Role, Constants.UDP_LOCAL_HOST, Constants.UDP_SERVER_PORT)

  private val committees, workerLeaders = Map[Int, List[String]]()
  private val txDigest = Map[Int, Map[Int, Int]]()
  private val finalAggTxDigest = Map[Int, Int]()
  private val shardDigest = Map[Int, Int]()
  private val proposedTxShardDigestList: ListBuffer[(String, Int)] = new ListBuffer[(String, Int)]()
  private var proposedTxDigest = 0
  private var proposedTxList: Map[Int, List[Int]] = null
  private var partnerAcrossCommittees:List[List[String]] = null
  private var totalNoOfCommittees:Int = 0
  private var workerLeaderCnt:Int = 0

  override def run(): Unit = {
    logger.log(Level.INFO, "ManagerLeader started")
    configureCommittees
    configurePartnersAcrossCommittees
    assignWorkerLeader
    prepareTxShards
    if (validateTxListAgainstDigest) {
      //If valid then move further
    }
    informWorkerLeader
    sendCommitteeDetails
    sendPartnerDetails
    waitForProcessedTxDigests
    sendFinalAggregatedTxDigest
    shutDown
    logger.log(Level.INFO, "ManagerLeader completed")
  }

  def printShardDigest = {
    logger.log(Level.INFO, "Printing shard digest on ManagerLeader side")
    shardDigest.foreach(keyVal => {
      logger.log(Level.INFO, "Key: " + keyVal._1 + " Value: " + keyVal._2)
    })
  }

  def prepareTxShards = {
    logger.info("Preparing Tx shards")
    val proposedTxMap: Map[Int, ListBuffer[String]] = Map[Int, ListBuffer[String]]()
    var reachedConsensus = false
    var maxCount = 0
    var toAskPorposedTxList = ListBuffer[String]()
    var ipList:ListBuffer[String] = null
    while (!reachedConsensus){
      proposedTxShardDigestList.foreach(keyVal => {
        ipList = proposedTxMap.getOrElse(keyVal._2, new ListBuffer[String]())
        ipList += keyVal._1
        proposedTxMap(keyVal._2) = ipList
        proposedTxShardDigestList -= keyVal
      })
      maxCount = 0
      toAskPorposedTxList.clear
      proposedTxMap.foreach(keyVal => {
        val cnt = keyVal._2.length
        if (cnt > maxCount) {
          toAskPorposedTxList = keyVal._2
          maxCount = cnt
          proposedTxDigest = keyVal._1
        }
      })
      if(maxCount >= Constants.CONSENSUS_MAJORITY){
        reachedConsensus = true
      } else {
        logger.info("Waiting for consensus of proposed tx shard digest")
        Thread.sleep(1234)
      }
    }
    proposedTxList = askForProposedTxListToBeProcessed(toAskPorposedTxList)
  }

  def validateTxListAgainstDigest: Boolean = {
    //validate proposedTxList against proposedTxDigest
    true
  }

  def configureCommittees: Unit = {
    logger.info("Configuring committees")
    val sql = Sql.nonCoreMiners
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    if (rs.isInstanceOf[ResultSet]) {
      val result = rs.asInstanceOf[ResultSet]
      var memberCnt = 0
      var cnt = 1
      var members = ListBuffer[String]()
      while (result.next) {
        if (memberCnt < Constants.COMMITTEE_SIZE) {
          members += result.getString(1)
          memberCnt += 1
        } else {
          committees(cnt) = members.toList
          //Initialize txDigest for each committee
          txDigest(cnt) = Map[Int, Int]()
          members = new ListBuffer[String]()
          cnt += 1
          memberCnt = 0
        }
      }
      committees(cnt) = members.toList
      //Initialize txDigest for each committee
      txDigest(cnt) = Map[Int, Int]()
    }
  }

  def configurePartnersAcrossCommittees = {
    var memberCnt = 0
    val finalList = ListBuffer[List[String]]()
    var partners = ListBuffer[String]()
    val random = Random
    while(memberCnt<Constants.COMMITTEE_SIZE){
      committees.foreach(keyVal => {
        try {
          partners += keyVal._2(memberCnt)
        } catch {
          case e:ArrayIndexOutOfBoundsException => {
            partners += keyVal._2(random.nextInt((keyVal._2.length-1)))
          }
          case e:Exception => {
            logger.log(Level.SEVERE, "Error while configuring the partners: " + e.getMessage)
          }
        }
      })
      finalList += partners.toList
      partners = ListBuffer[String]()
      memberCnt+=1;
    }
    partnerAcrossCommittees = finalList.toList
  }

  def onReceiveOfProposedTxDigest(hostName: String, digest: Int) = {
    logger.info("Proposed tx digest from " + hostName + " is " + digest)
    proposedTxShardDigestList += ((hostName, digest))
  }

  def assignWorkerLeader: Unit = {
    logger.info("Assigning worker leaders to committees from managers")
    setWorkerLeaderCnt
    val sql = Sql.coreFollowers
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    if (rs.isInstanceOf[ResultSet]) {
      val result = rs.asInstanceOf[ResultSet]
      var memberCnt = 0
      var cnt = 1
      var leaders = ListBuffer[String]()
      while (result.next) {
        if (memberCnt <= workerLeaderCnt) {
          leaders += result.getString(1)
          memberCnt += 1
        } else {
          workerLeaders(cnt) = leaders.toList
          leaders = new ListBuffer[String]()
          cnt += 1
          memberCnt = 0
        }
      }
      workerLeaders(cnt) = leaders.toList
    }
  }

  def setWorkerLeaderCnt = {
    logger.info("Setting worker leader count and number of committees")
    val sql = Sql.nonCoreMinersCnt
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    if (rs.isInstanceOf[ResultSet]) {
      val result = rs.asInstanceOf[ResultSet]
      result.next
      totalNoOfCommittees = floor(result.getInt(1)/Constants.COMMITTEE_SIZE).toInt
      workerLeaderCnt = floor(Constants.COMMITTEE_SIZE/totalNoOfCommittees).toInt
    }
    logger.info("Set worker leader count: " + workerLeaderCnt + " and number of committees: " + totalNoOfCommittees)
  }

  def informWorkerLeader: Unit = {
    logger.info("Informing managers about its worker leader role")
    var cmt: Int = 0
    workerLeaders.foreach((keyVal) => {
      cmt = keyVal._1
      keyVal._2.foreach(member => {
        val sign = Crypto.generateSignature(cmt, this.privateKey)
        val msg = new Message(role, MessageType.INFO, MessageTag.ASSIGN_WORKER_LEADER, cmt, sign)
        udpClient.sendMessage(msg, member, peerAddress.get((member, Role.CORE_FOLLOWER.id)).get)
      })
    })
  }

  def sendCommitteeDetails = {
    logger.info("Sending committee member list to managers")
    var message: Message = null
    committees.foreach(keyVal => {
      val sign = Crypto.generateSignature(keyVal._2, privateKey)
      message = new Message(role, MessageType.INFO, MessageTag.COMMITTEE_MEMBERS, keyVal._2, sign)
      workerLeaders.get(keyVal._1).get.foreach(leader => {
        udpClient.sendMessage(message, leader, peerAddress((leader, Role.CORE_FOLLOWER.id)))
      })
    })
    logger.info("Sent committee member list to managers")
  }

  def sendPartnerDetails = {
    logger.info("Sending partner details to managers")
    var message: Message = null
    val sign = Crypto.generateSignature(partnerAcrossCommittees, privateKey)
    message = new Message(role, MessageType.INFO, MessageTag.PARTNERS_ACROSS_COMMITTEES, partnerAcrossCommittees, sign)
    workerLeaders.foreach(keyVal => {
      keyVal._2.foreach(leader => {
        udpClient.sendMessage(message, leader, peerAddress((leader, Role.CORE_FOLLOWER.id)))
      })
    })
    logger.info("Sent partner details to managers")
  }

  def waitForProcessedTxDigests = {
    var reachedConsensus:Boolean = false
    while (!reachedConsensus) {
      Thread.sleep(1234)
      reachedConsensus=true
      txDigest.foreach(keyVal => {
        if(keyVal._2.isEmpty){
          reachedConsensus = false
        } else {
          var majority = false
          keyVal._2.foreach(digestCnt => {
            if (digestCnt._2 >= Constants.CONSENSUS_MAJORITY)
              majority = true
          })
          if (!majority)
            reachedConsensus = false
        }
      })
    }
  }

  def onReceiveOfShardDigest(digest: Int) = {
    logger.info("Received digest value of proposed shards: " + digest)
    val cnt = shardDigest.getOrElse(digest, 0) + 1
    shardDigest(digest) = cnt
  }

  def onReceiveOfProcessedTxDigest(ipAddr: String, digest: Int) = {
    logger.info("Received digest value: " + digest + " of processed shard from host: " + ipAddr)
    var cmt: Int = 0
    workerLeaders.foreach(keyVal => {
      if (keyVal._2.contains(ipAddr)) {
        cmt = keyVal._1
      }
    })
    val cnt = txDigest.getOrElse(cmt, Map[Int, Int]()).getOrElse(digest, 0) + 1
    //logger.info("COUNT VALUE IS: " + cnt + " CMT VALUE IS: " + cmt.asInstanceOf[Int])
    txDigest(cmt)(digest) = cnt
    logger.info("Set digest value: " + digest + " of processed committee " + cmt + " from host: " + ipAddr)
  }

  def calculateFinalAggregatedTxDigest = {
    logger.info("Set final agg tx digest values")
    var cmt = 0
    var maxDigest = 0
    var maxDigestCnt = 0
    txDigest.foreach(keyVal => {
      cmt = keyVal._1
      keyVal._2.foreach(digestCountPair => {
        if (maxDigestCnt < digestCountPair._2)
          maxDigest = digestCountPair._1
        maxDigestCnt = digestCountPair._2
      })
      finalAggTxDigest(cmt) = maxDigest
      maxDigest = 0
      maxDigestCnt = 0
    })
  }

  def sendFinalAggregatedTxDigest = {
    calculateFinalAggregatedTxDigest
    logger.info("Sending following final agg tx digests to all managers")
    finalAggTxDigest.foreach(keyVal => {
      logger.info("Committee/Shard id: " + keyVal._1 + " digest value: " + keyVal._2)
    })

    val sign = Crypto.generateSignature(finalAggTxDigest, this.privateKey)
    val message = new Message(role, MessageType.INFO, MessageTag.FINAL_AGG_PROCESSED_TX_DIGEST, finalAggTxDigest, sign)
    workerLeaders.foreach(keyVal => {
      keyVal._2.foreach(manager => {
        udpClient.sendMessage(message, manager, peerAddress.get((manager, Role.CORE_FOLLOWER.id)).get)
      })
    })
    logger.info("Sent final agg tx digests to all managers")
  }

  def getFinalAggregatedTxDigest = {
    finalAggTxDigest
  }

  def askForProposedTxListToBeProcessed(hostList: ListBuffer[String]): Map[Int, List[Int]] = {
    logger.info("Asking managers to propose Tx list to be processed")
    val msg = new Message(role, MessageType.REQUEST, MessageTag.PROPOSE_TX_LIST_TO_BE_PROCESSED)
    hostList.foreach(host => {
      val port = peerAddress.get((host, Role.CORE_FOLLOWER.id)).get
      return udpClient.sendMessage(msg, host, port, true).message.asInstanceOf[Map[Int, List[Int]]]
    })
    null
  }

  def askForProposedTxListToBeProcessed(shard: Int, host: String, port: Int): Map[Int, List[Int]] = {
    logger.info("Asking managers to propose Tx list to be processed for shard: " + shard)
    val sign = Crypto.generateSignature(shard, privateKey)
    val msg = new Message(role, MessageType.REQUEST, MessageTag.PROPOSE_TX_LIST_TO_BE_PROCESSED, shard, sign)
    return udpClient.sendMessage(msg, host, port, true).message.asInstanceOf[Map[Int, List[Int]]]
  }

  override def processInfoMessage(message: Message, host: String, port: Int): Unit = {
    message.messageTag match {
      case MessageTag.PROPOSE_TX_LIST_DIGEST_TO_BE_PROCESSED => {
        logger.info("Received an information message of Tag: " + MessageTag.PROPOSE_TX_LIST_DIGEST_TO_BE_PROCESSED)
        val digest = message.message.asInstanceOf[Int]
        onReceiveOfProposedTxDigest(host, digest)
      }
      case MessageTag.PROCESSED_TX_DIGEST => {
        logger.info("Received an information message of Tag: " + MessageTag.PROCESSED_TX_DIGEST)
        val digest = message.message.asInstanceOf[Int]
        onReceiveOfProcessedTxDigest(host, digest)
      }
      case MessageTag.PUBLIC_KEY => {
        logger.info("Received an information message of Tag: " + MessageTag.PUBLIC_KEY)
        PublicKeyHolder.setPublicKey(host, message.role, message.message.asInstanceOf[PublicKey])
      }
      case MessageTag.SHARD_DIGEST => {
        logger.info("Received an information message of Tag: " + MessageTag.SHARD_DIGEST)
        onReceiveOfShardDigest(message.message.asInstanceOf[Int])
      }
    }
  }

  override def processRequestMessage(message: Message, host: String, port: Int): Unit = {
    var response:Any = null
    var responseFlg:Boolean = true
    message.messageTag match {
      case MessageTag.PROCESSED_TX_DIGEST => {
        logger.info("Received a request of Tag: " + MessageTag.PROCESSED_TX_DIGEST)
        val digest = getFinalAggregatedTxDigest
        response = digest
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
}