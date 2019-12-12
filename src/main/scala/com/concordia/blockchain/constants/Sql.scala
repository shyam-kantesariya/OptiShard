package com.concordia.blockchain.constants

import com.concordia.blockchain.constants.Role.Role

object Sql {
  val publicKeyFilepath = "SELECT public_key_filepath FROM miner WHERE ip_addr = "
  val accountCount = "SELECT debit_acc_id, SUM(1) AS cnt FROM transaction GROUP BY debit_acc_id ORDER BY " +
    "debit_acc_id"
  val nonCoreMiners = "SELECT ip_addr FROM miner WHERE isblocked=0 AND role_id=4"
  val nonCoreMinersCnt = "SELECT count(*) FROM miner WHERE isblocked=0 AND role_id=4"
  val coreFollowers = "SELECT ip_addr FROM miner WHERE isblocked=0 AND role_id=2"
  val coreFollowerCnt = "SELECT COUNT(*) FROM miner WHERE isblocked=0 AND role_id=2"
  val allMiners = "SELECT ip_addr, role_id, port FROM miner WHERE isblocked = 0"
  val coreLeader = "SELECT ip_addr, port FROM miner WHERE isblocked = 0 AND role_id=1"
  val truncateUserTxTable =  "truncate usertx"
  //val userTxCnt = "SELECT t1.debit_acc_id, t1.tx_id  FROM usertx t1 JOIN (SELECT debit_acc_id, count(*) cnt FROM usertx " +
  //  "GROUP BY debit_acc_id) t2 ON t1.debit_acc_id = t2.debit_acc_id ORDER BY t2.cnt DESC"
  val userTxCnt = "SELECT debit_acc_id, count(*) cnt FROM usertx GROUP BY debit_acc_id ORDER BY count(*) DESC"
  //Columns
  val colIpAddr = "ip_addr"
  val colPort = "port"
  val colRoleId = "role_id"
  val colTxId = "tx_id"
  val colAccId = "acc_id"
  val colBal = "balance"

  def readBalance(accList:List[Int]):String = {
    "SELECT acc_id, balance FROM account WHERE acc_id IN (" + accList.mkString(",") + ")"
  }

  def getRegistrationStatement(ip:String, path:String, role: Role):String = {
    "UPDATE miner SET public_key=\"" + path + "\" WHERE ip_addr=\"" + ip + "\" AND role_id=" + role.id
  }

  def getRegistrationStatement(ip:String, port:Int, path:String, role: Role):String = {
    "UPDATE miner SET port=" + port + ", public_key=\"" + path + "\" WHERE ip_addr=\"" + ip + "\" AND role_id=" + role.id
  }

  def getTxDetails(transactions:List[Int]):String = {
    "SELECT tx_id, credit_acc_id, debit_acc_id, amount FROM transaction WHERE tx_id IN (" + transactions.mkString(",") + ")"
  }

  def getTxForUsers(users:List[Int]):String = {
    "SELECT tx_id FROM transaction WHERE debit_acc_id IN (" + users.mkString(",") + ")"
  }

  def setBalance(acc_id:Int, balance:Double):String = {
    "UPDATE account SET balance="+ balance + "WHERE acc_id =" + acc_id
  }

  def populateUserTxTable(txList:List[Int]):String = {
    "INSERT INTO usertx (debit_acc_id, tx_id) SELECT debit_acc_id, tx_id FROM transaction WHERE tx_id IN (" + txList.mkString(",") + ")"
  }

}