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
import scala.math.{ceil, floor}

class Manager(role: Role, myPublicIp:String, port:Int)
  extends Node(role, myPublicIp, port) with Runnable{
  def this(role: Role) = this(role: Role, Constants.UDP_LOCAL_HOST, Constants.UDP_SERVER_PORT)
  private val shards:Map[Int, List[Int]] = Map[Int, List[Int]]()
  private var txListToProcess:List[Int] = List[Int]()
  private var leaderOfCommittee:Int = 0
  private var committeeMembers:List[String] = List[String]()
  private var partnerAcrossCommittees:List[List[String]] = null
  private var workerLeader = new WorkerLeader(Role.NON_CORE_LEADER, Constants.UDP_LOCAL_HOST, 12347)
  private var processedTxDigestHostList:Tuple2[Int,List[String]] = null
  private var leader:String = null
  private var finalAggProcessedTxDigest:Map[Int,Int] = Map[Int,Int]()
  private var processedTxDigest = 0
  private var totalNoOfCommittees:Int = 0

  override def run(): Unit = {
    logger.log(Level.INFO, "Manager started")
    setLeader
    setTotalNoOfCommittees
    shardTransactions
    sendProposedTxShardDigest(leader)
    while (txListToProcess.isEmpty) {
      Thread.sleep(1234)
    }
    workerLeader.setManager(this)
    workerLeader.setTxListToBeProcessed(txListToProcess)
    while(committeeMembers.isEmpty){
      logger.info("Waiting for committee member list")
      Thread.sleep(1234)
    }
    workerLeader.setTeam(committeeMembers)
    while (partnerAcrossCommittees.isEmpty){
      logger.info("Waiting for partner details")
      Thread.sleep(1234)
    }
    workerLeader.setPartnerDetails(partnerAcrossCommittees)
    workerLeader.setLeaderOfCommittee(leaderOfCommittee)
    val worker = new Thread(workerLeader)
    worker.start
    sendProcessedTxDigestToLeader
    while (finalAggProcessedTxDigest.isEmpty) {
      Thread.sleep(1234)
    }
    worker.join
    shutDown
    logger.log(Level.INFO, "Manager completed")
  }

  def setLeader = {
    logger.log(Level.INFO, "Set leader to " + leader)
    val sql = Sql.coreLeader
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    if (rs.isInstanceOf[ResultSet]){
      var result = rs.asInstanceOf[ResultSet]
      result.next()
      this.leader = result.getString(Sql.colIpAddr)
    }
  }

  def setTotalNoOfCommittees = {
    logger.info("Setting total number of committees")
    val sql = Sql.nonCoreMinersCnt
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    if (rs.isInstanceOf[ResultSet]) {
      val result = rs.asInstanceOf[ResultSet]
      result.next
      totalNoOfCommittees = floor(result.getInt(1)/Constants.COMMITTEE_SIZE).toInt
    }
    logger.info("Set total number of committees: " + totalNoOfCommittees)
  }

  def getTransactionsToProcess(shards:List[Int]): List[Int] = {
    txListToProcess
  }

  def shardTransactions = {
    logger.log(Level.INFO, "Preparing Tx shards")
    var sql = Sql.accountCount
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    var count = 0
    var users = Map[Int,Int]()

    logger.info("Geeting pending transactions for processing")
    if (rs.isInstanceOf[ResultSet]){
      var result = rs.asInstanceOf[ResultSet]
      while (result.next) {
        count += result.getInt(2)
        users(result.getInt(1)) = result.getInt(2)
      }
    }
    val txCount:Int = ceil(count/totalNoOfCommittees).toInt
    setUserShards(users, txCount)

    logger.info("Set transactions for shard")
    shards.foreach(shardUsers => {
      shards(shardUsers._1) = getTxForUsers(shardUsers._2)
      logger.info("Shard: " + shardUsers._1 + " Tx: " + shards(shardUsers._1).length)
    })
  }

  def setUserShards(users:Map[Int,Int], txCount:Int)={
    var cnt = 0
    var shardId = 1
    var userShard = ListBuffer[Int]()
    logger.info("Set users for shard")
    users.foreach((userTx) => {
      if(cnt<txCount){
        cnt += userTx._2
        userShard += userTx._1
      } else {
        logger.info("Shard id: " + shardId + " users: " + userShard.toList.mkString(","))
        shards(shardId) = userShard.toList
        userShard.clear
        shardId += 1
        cnt=userTx._2
        userShard += userTx._1
      }
    })
    logger.info("Shard id: " + shardId + " users: " + userShard.toList.mkString(","))
    shards(shardId) = userShard.toList
  }

  def getTxForUsers(users:List[Int]): List[Int]={
    val sql = Sql.getTxForUsers(users)
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    val list:ListBuffer[Int] = ListBuffer[Int]()
    if (rs.isInstanceOf[ResultSet]){
      var result = rs.asInstanceOf[ResultSet]
      while (result.next) {
        list += result.getInt(Sql.colTxId)
      }
    }
    logger.info("Transactions for users: " + users.mkString(",") + " are as following: " + list.mkString(","))
    list.toList
  }

  def onAssignOfWorkerLeader(committee:Int) = {
    logger.info("Assigned leader for committee: " + committee)
    leaderOfCommittee = committee
    txListToProcess = shards(leaderOfCommittee)
  }

  def onReceiveOfCommitteeMembers(members:List[String]) = {
    logger.info("Received committee members from leader")
    committeeMembers = members
  }

  def onReceiveOfPartnersAcrossCommittees(partners:List[List[String]]) = {
    logger.info("Received partner details across committees from leader")
    partnerAcrossCommittees = partners
  }

  def getProposedTxShardDigest:Map[Int,Int] = {
    val shardDigest:Map[Int,Int] = Map()
    shards.foreach((shard) => {
      shardDigest(shard._1) = shard._2.hashCode()
    })
    shardDigest
  }

  def getTxListOfShard(shardId:Int):Map[Int, List[Int]] = {
    val response = Map[Int, List[Int]]()
    response(shardId) = shards(shardId)
    response
  }

  def getTxListOfShard(): Map[Int, List[Int]] = {
    shards
  }

  def setProcessedTxDigestHostList(digestHostList:Tuple2[Int, List[String]]) = {
    processedTxDigestHostList = digestHostList
    processedTxDigest = digestHostList._1
    logger.info("Processed Tx digest is: " + processedTxDigest)
  }

  def sendProposedTxShardDigest(host:String) = {
    logger.info("Sending proposed shard tx digest")
    val shardDigest = getProposedTxShardDigest
    val sign = Crypto.generateSignature(shardDigest, privateKey)
    val msg = new Message(role, MessageType.INFO,MessageTag.PROPOSE_TX_LIST_DIGEST_TO_BE_PROCESSED, shardDigest.hashCode, sign)
    udpClient.sendMessage(msg, host, peerAddress.get((leader, Role.CORE_LEADER.id)).get)
    logger.log(Level.INFO, "Sent proposed shard tx digest to leader")
  }

  def sendProcessedTxDigestToLeader = {
    while(processedTxDigest == 0) {
      logger.info("Waiting for processedTxDigest from worker leader")
      Thread.sleep(1234)
    }
    logger.info("Sending processed tx digest to core leader. Digest value: " + processedTxDigest)
    val sign = Crypto.generateSignature(processedTxDigest, privateKey)
    val msg = new Message(role, MessageType.INFO, MessageTag.PROCESSED_TX_DIGEST, processedTxDigest, sign)
    udpClient.sendMessage(msg, leader, peerAddress.get((leader, Role.CORE_LEADER.id)).get)
    logger.info("Sent processed tx digest to core leader")
  }

  def setFinalAggProcessedTxDigest(aggMap:Map[Int, Int]) = {
    logger.info("Sending final agg digest to WorkerLeader")
    finalAggProcessedTxDigest = aggMap
    workerLeader.setFinalAggProcessedTxDigest(aggMap)
    logger.info("Sent final agg digest to WorkerLeader")
  }

  override def processInfoMessage(message: Message, host: String, port: Int): Unit = {
    message.messageTag match {
      case MessageTag.ASSIGN_WORKER_LEADER => {
        logger.info("Received an information message of Tag: " + MessageTag.ASSIGN_WORKER_LEADER)
        val committee = message.message.asInstanceOf[Int]
        onAssignOfWorkerLeader(committee)
      }
      case MessageTag.COMMITTEE_MEMBERS => {
        logger.info("Received an information message of Tag: " + MessageTag.COMMITTEE_MEMBERS)
        onReceiveOfCommitteeMembers(message.message.asInstanceOf[List[String]])
      }
      case MessageTag.PARTNERS_ACROSS_COMMITTEES => {
        logger.info("Received an information message of Tag: " + MessageTag.PARTNERS_ACROSS_COMMITTEES)
        onReceiveOfPartnersAcrossCommittees(message.message.asInstanceOf[List[List[String]]])
      }
      case MessageTag.PUBLIC_KEY => {
        logger.info("Received an information message of Tag: " + MessageTag.PUBLIC_KEY)
        PublicKeyHolder.setPublicKey(host, message.role, message.message.asInstanceOf[PublicKey])
      }
      case MessageTag.FINAL_AGG_PROCESSED_TX_DIGEST => {
        logger.info("Received an information message of Tag: " + MessageTag.FINAL_AGG_PROCESSED_TX_DIGEST)
        setFinalAggProcessedTxDigest(message.message.asInstanceOf[Map[Int, Int]])
      }
    }
  }

  override def processRequestMessage(message: Message, host: String, port: Int): Unit = {
    var response:Any = null
    var responseFlg:Boolean = true
    message.messageTag match {
      case MessageTag.PROPOSE_TX_LIST_TO_BE_PROCESSED => {
        logger.info("Received a request of Tag: " + MessageTag.PROPOSE_TX_LIST_TO_BE_PROCESSED)
        val txShardTxList = getTxListOfShard
        response = txShardTxList
      }
      case MessageTag.SHARD_DIGEST => {
        logger.info("Received a request of Tag: " + MessageTag.SHARD_DIGEST)
        val shardDigest = getProposedTxShardDigest
        response = shardDigest
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