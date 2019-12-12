package com.concordia.blockchain.model.node

import java.security.PublicKey
import java.sql.ResultSet
import java.util.logging.Level

import com.concordia.blockchain.conf.PublicKeyHolder
import com.concordia.blockchain.constants.MessageTag.MessageTag

import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map, Set}
import com.concordia.blockchain.model.Transaction
import com.concordia.blockchain.model.udp.Message
import com.concordia.blockchain.constants._
import com.concordia.blockchain.constants.Role.Role
import com.concordia.blockchain.crypto.Crypto

import scala.math.{min, floor};

//Ref: https://www.scala-lang.org/old/node/125
class Worker(role:Role, myPublicIp:String, port:Int) extends Node(role, myPublicIp, port) with Runnable{
  def this(role: Role) = this(role: Role, Constants.UDP_LOCAL_HOST, Constants.UDP_SERVER_PORT)
  var transactions:List[Int] = List[Int]()
  val approvedTxList, rejectedTxList = ListBuffer[Int]()
  var finalAggProcessedTxDigest:Map[Int,Int] = Map[Int,Int]()
  var leaders:ListBuffer[String] = ListBuffer[String]()
  var partners:Set[String] = Set()
  var committeeId:Int = -1
  var availableForHelp = false
  var userSeq:ArrayBuffer[Int] = ArrayBuffer[Int]()
  var userTxList:Map[Int, ListBuffer[Transaction]] = Map[Int, ListBuffer[Transaction]]()
  var txProcessedBy:Map[String, ListBuffer[Int]] = Map[String, ListBuffer[Int]]()
  var txProcessedCnt = 0
  val offLoadingInProgress = ListBuffer[Int]()
  val offLoaded:Map[String, ListBuffer[Int]] = Map[String, ListBuffer[Int]]()
  var onLoaded:Tuple2[String, List[Int]] = null

  override def run(): Unit = {
    logger.log(Level.INFO, "Worker started")
    processTransactions
    sendProcessedTxDigest
    while(finalAggProcessedTxDigest.isEmpty){
      logger.info("Waiting for final agg list of processed Tx")
      //Ask for work from partners
      helpOfferRoutine
      Thread.sleep(1234)
    }
    availableForHelp = false
    //Apply TX from peers: PENDING
    shutDown
    logger.log(Level.INFO,"Worker completed")
  }

  def getTransactionsToProcess:Unit={
    logger.info("Get Tx list to process")
    val request = new Message(role, MessageType.REQUEST, MessageTag.TX_LIST_TO_BE_PROCESSED)
    val responseMessage = udpClient.sendMessage(request,leaders(0), peerAddress.get((leaders(0), Role.NON_CORE_LEADER.id)).get, true)
    transactions = responseMessage.message.asInstanceOf[List[Int]]
  }

  def setTranscationsToProcess(cmtId:Int, txList:List[Int]) = {
    logger.info("Set Tx list to process as: " + txList.mkString(","))
    transactions = txList
    committeeId = cmtId
  }

  def setLeader(leaderHostName:String) = {
    logger.info("Set leader to: " + leaderHostName)
    leaders += leaderHostName
  }

  def setPartnersAcrossCommittees(partnerList:List[List[String]]) = {
    logger.info("Setting partners across committees")
    partnerList.foreach(partnerElement => {
      if(partnerElement.contains(myPublicIp))
        partnerElement.foreach(partner => {
          partners += partner
        })
    })
    logger.info("Set partners as: " + partners.mkString(","))
    partners -= myPublicIp
    logger.info("Set partners as: " + partners.mkString(","))
  }

  def processTransactions:Unit={
    while (transactions.isEmpty){
      logger.info("Waiting for Tx list to be processed")
      Thread.sleep(1234)
    }
    fetchTxDetails
    logger.info("Processing Tx")
    txProcessedBy(myPublicIp) = ListBuffer[Int]()
    var list:ListBuffer[Int] = null
    var elem:Int = 0
    while (!userSeq.isEmpty) {
      elem = userSeq.head
      list = txProcessedBy.get(myPublicIp).get
      list += elem
      userTxList.get(elem).foreach(txList => {
        txList.foreach(tx => validateTx(tx))
        txProcessedCnt += 1
      })
      userSeq -= elem
    }
    logger.info("Approved Tx: " + approvedTxList.mkString(","))
    logger.info("Rejected Tx: " + rejectedTxList.mkString(","))
  }

  def fetchTxDetails = {
    logger.info("Fetching Tx details")
    var cnt=0
    var sql:String = null
    var tx:Transaction = null
    var txList:ListBuffer[Transaction] = null
    var txCnt = 0
    truncateUserTxTable
    while (cnt < transactions.length){
      sql = Sql.getTxDetails(transactions.slice(cnt, min(cnt+Constants.MAX_TX_READ,transactions.length)))
      val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
      if (rs.isInstanceOf[ResultSet]){
        var result = rs.asInstanceOf[ResultSet]
        while (result.next){
          tx = new Transaction(result.getInt(1), result.getInt(2), result.getInt(3),
            result.getDouble(4), this.getLogger)
          txList = userTxList.getOrElse(result.getInt(3), new ListBuffer[Transaction])
          txList += tx
          userTxList(result.getInt(3)) = txList
        }
      }
      populateUserTxTable(transactions.slice(cnt, min(cnt+Constants.MAX_TX_READ,transactions.length)))
      cnt = min(cnt+Constants.MAX_TX_READ, transactions.length)
    }
    setUserTxCnt
    logger.info("Fetched Tx details")
  }
  def truncateUserTxTable = {
    val sql = Sql.truncateUserTxTable
    mySql.executeQuery(SqlOperation.DELETE, sql)
  }

  def populateUserTxTable(txList:List[Int])={
    val sql = Sql.populateUserTxTable(txList)
    mySql.executeQuery(SqlOperation.INSERT, sql)
  }

  def setUserTxCnt = {
    val sql = Sql.userTxCnt
    val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
    if (rs.isInstanceOf[ResultSet]){
      var result = rs.asInstanceOf[ResultSet]
      while (result.next){
        userSeq += result.getInt(1)
      }
    }
  }

  def validateTx(tx:Transaction) = {
    if(tx.checkTransaction(mySql))
      approvedTxList += tx.txId
    else
      rejectedTxList += tx.txId
  }

  def getProcessedTxList:List[Int]={
    logger.info("Asking for processed Tx list")
    approvedTxList.toList.sorted
  }

  def getProcessedTxDigest:Int={
    getProcessedTxList.hashCode
  }

  def sendProcessedTxDigest = {
    val digest = getProcessedTxDigest
    logger.info("Sending processed Tx digest: " + digest + " to leaders: " + leaders.toList.mkString(","))
    val sign = Crypto.generateSignature(digest, privateKey)
    val msg = new Message(role, MessageType.INFO, MessageTag.PROCESSED_TX_DIGEST, digest, sign)
    leaders.foreach(leader => {
      udpClient.sendMessage(msg, leader, peerAddress.get((leader, Role.NON_CORE_LEADER.id)).get)
    })
  }

  def helpOfferRoutine = {
    if(onLoaded == null){
      availableForHelp = true
      offerHelpToPartners
    }
    else {
      availableForHelp = false
      val txList:ListBuffer[Transaction] = ListBuffer[Transaction]()
      val sql = Sql.getTxDetails(onLoaded._2)
      val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
      var tx:Transaction = null
      val approvedTx:ListBuffer[Int] = ListBuffer[Int]()
      if (rs.isInstanceOf[ResultSet]){
        var result = rs.asInstanceOf[ResultSet]
        while (result.next){
          tx = new Transaction(result.getInt(1), result.getInt(2), result.getInt(3),
            result.getDouble(4), this.getLogger)
          txList += tx
        }
      }
      txList.foreach(tx => {
        if(tx.checkTransaction(mySql))
          approvedTx += tx.txId
      })
      val sign = Crypto.generateSignature(approvedTx.toList, privateKey)
      val msg = new Message(role, MessageType.INFO, MessageTag.WORK_SHARING, approvedTx.toList, sign)
      udpClient.sendMessage(msg, onLoaded._1, peerAddress.get((onLoaded._1, Role.NON_CORE_FOLLOWER.id)).get)
      availableForHelp = true
      onLoaded = null
    }
  }

  def offerHelpToPartners = {
    val msg = new Message(role, MessageType.INFO, MessageTag.OFFER_HELP)
    partners.foreach(partner => {
      udpClient.sendMessage(msg, partner, peerAddress.get((partner, Role.NON_CORE_FOLLOWER.id)).get)
    })
  }

  def onReceiveOfHelpOffer(ip:String, role:Role):Unit = {
    val txCntToOffLoad = floor((transactions.length - txProcessedCnt)/10)
    if(txCntToOffLoad < Constants.MIN_TX_TO_OFFLOAD)
      return
    val sendingUserTx = ListBuffer[Int]()
    var sendingTx = ListBuffer[Int]()
    var cnt=0
    var usr=0
    while(!userSeq.isEmpty && cnt<txCntToOffLoad && offLoaded.get(ip)==None){
      usr = userSeq(userSeq.length-1)
      cnt += userTxList.get(usr).get.length
      sendingUserTx += usr
      offLoadingInProgress += usr
    }
    sendingUserTx.foreach(user => {
      userTxList.get(user).get.foreach(tx => sendingTx += tx.txId)
    })
    val sign = Crypto.generateSignature(sendingTx.toList, privateKey)
    val msg = new Message(role, MessageType.REQUEST, MessageTag.WORK_SHARING, sendingTx.toList, sign)
    val accepted:Boolean = udpClient.sendMessage(msg, ip, peerAddress.get((ip, role.id)).get, true).message.asInstanceOf[Boolean]
    var lbf:ListBuffer[Int] = ListBuffer[Int]()
    if (accepted){
      sendingUserTx.foreach(user => {
        offLoadingInProgress -= user
        lbf += user
        offLoaded(ip) = lbf
      })
    }
  }

  def onReceiveOfHelpRequest(message: Message, host:String):Boolean = {
    if(!availableForHelp)
      false
    availableForHelp = false
    onLoaded = (host, message.message.asInstanceOf[List[Int]])
    true
  }

  def onReceiveOfProcessedHelp(host: String, listTx: List[Int]) = {
    listTx.foreach(tx => approvedTxList += tx)
    offLoaded.get(host).get.foreach(user => userSeq -= user)
    offLoaded.remove(host)
  }

  def setFinalAggProcessedTxDigest(aggMap:Map[Int, Int]) = {
    logger.info("Set final agg processed Tx digest")
    finalAggProcessedTxDigest = aggMap
    printFinalAggProcessedTxDigest
  }

  def printFinalAggProcessedTxDigest = {
    finalAggProcessedTxDigest.foreach(keyVal => {
      logger.info("Committee/Shard id: " + keyVal._1 + " digest value: " + keyVal._2)
    })
  }

  override def processInfoMessage(message: Message, host: String, port: Int): Unit = {
    message.messageTag match {
      case MessageTag.COMMITTEE_LEADER => {
        logger.info("Received an information message of Tag: " + MessageTag.COMMITTEE_LEADER)
        setLeader(host)
      }
      case MessageTag.PARTNERS_ACROSS_COMMITTEES => {
        logger.info("Received an information message of Tag: " + MessageTag.PARTNERS_ACROSS_COMMITTEES)
        setPartnersAcrossCommittees(message.message.asInstanceOf[List[List[String]]])
      }
      case MessageTag.TX_LIST_TO_BE_PROCESSED => {
        logger.info("Received an information message of Tag: " + MessageTag.TX_LIST_TO_BE_PROCESSED)
        val msg = message.message.asInstanceOf[(Int, List[Int])]
        setTranscationsToProcess(msg._1, msg._2)
      }
      case MessageTag.PUBLIC_KEY => {
        logger.info("Received an information message of Tag: " + MessageTag.PUBLIC_KEY)
        PublicKeyHolder.setPublicKey(host, message.role, message.message.asInstanceOf[PublicKey])
      }
      case MessageTag.OFFER_HELP => {
        logger.info("Received an information message of Tag: " + MessageTag.OFFER_HELP)
        onReceiveOfHelpOffer(host, message.role)
      }
      case MessageTag.WORK_SHARING => {
        logger.info("Received an information message of Tag: " + MessageTag.WORK_SHARING)
        onReceiveOfProcessedHelp(host, message.message.asInstanceOf[List[Int]])
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
      case MessageTag.PROCESSED_TX_DIGEST => {
        logger.info("Received a request of Tag: " + MessageTag.PROCESSED_TX_DIGEST)
        val digest = getProcessedTxDigest
        response = digest
      }
      case MessageTag.PROCESSED_TX_LIST => {
        logger.info("Received a request of Tag: " + MessageTag.PROCESSED_TX_LIST)
        val txList = getProcessedTxList
        response = txList
      }
      case MessageTag.WORK_SHARING => {
        logger.info("Received a request of Tag: " + MessageTag.WORK_SHARING)
        response = onReceiveOfHelpRequest(message, host)
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