package com.concordia.blockchain.model

import java.sql.ResultSet
import java.util.logging.{Level, Logger}

import com.concordia.blockchain.model.database.MySql
import com.concordia.blockchain.constants.{Sql, SqlOperation}

import scala.collection.mutable.Map

class Transaction(val txId:Int, val creditAccount:Int, val debitAccount:Int, val amount:Double, logger:Logger) {
  val accounts = Map[Int, Double]()
  def checkTransaction(mySql: MySql): Boolean = {

    var sql = Sql.readBalance(List(creditAccount, debitAccount))

    try {
      val rs = mySql.executeQuery(SqlOperation.SELECT, sql)
      if (rs.isInstanceOf[ResultSet]) {
        var result = rs.asInstanceOf[ResultSet]
        while (result.next) {
          accounts(result.getInt(Sql.colAccId)) = result.getDouble(Sql.colBal)
        }
      }

      if ((accounts.get(debitAccount).get - amount) >= 0) {
        return true
      } else {
        logger.info("Rejected tx: " + this.toString)
        return false
      }
    } catch {
      case e: Exception => {
        logger.log(Level.SEVERE, "Error while fetching balance for tx: " + txId)
        return false
      }
    }
  }

  override def toString: String = {
    if (accounts.isEmpty)
      super.toString
    else
      "txId:" + txId + " Credit account: " + creditAccount + " Debit account: " + debitAccount + " Debit account balance: " +
      accounts.get(debitAccount).get + " Tx amount: " + amount
  }

  def applyTransaction(mySql: MySql): Boolean = {
    try {
      var sql = Sql.setBalance(debitAccount, (accounts.get(debitAccount).get - amount))
      mySql.executeQuery(SqlOperation.UPDATE, sql)
      sql = Sql.setBalance(creditAccount, (accounts.get(creditAccount).get + amount))
      mySql.executeQuery(SqlOperation.UPDATE, sql)
      true
    } catch {
      case e: Exception => {
        logger.log(Level.SEVERE, "Error while fetching balance for tx: " + txId)
        false
      }
    }
  }
}