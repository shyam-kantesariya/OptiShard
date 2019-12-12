package com.concordia.blockchain.model.database

import java.sql.{Connection, DriverManager, Statement}
import java.util.logging.{Level, Logger}

import com.concordia.blockchain.constants.Constants._
import com.concordia.blockchain.constants.{Constants, SqlOperation}
import com.concordia.blockchain.constants.SqlOperation.SqlOperation
import com.concordia.blockchain.model.node.Node;

class MySql (private val url:String, logger:Logger) {
  def this(logger:Logger) = this(MYSQL_CONN_STRING, logger)
  private val username = MYSQL_USER_ID
  private val password = MYSQL_PASSWORD
  private var connection:Connection = _
  private var statement:Statement = _
  private var connEstablished = false

  def openConnection:Int = {
    if (connEstablished)
      return 1
    try {
      Class.forName(Constants.MYSQL_DRIVER).newInstance
      connection = DriverManager.getConnection(url, username, password)
      statement = connection.createStatement
      connEstablished = true
      logger.info("Opened connection to database")
      return 1
    } catch {
      case e: Exception => {
        logger.log(Level.SEVERE, "Error while opening connection to database: " + e.printStackTrace)
      }
    }
    return 0
  }

  def closeConnection:Int = {
    try{
      connection.close
      connEstablished = false
      return 1
    }
    catch {
      case e: Exception => {
        println("Error while closing the connection")
        e.printStackTrace
      }
    }
    return 0
  }

  def executeQuery(operation:SqlOperation, sql:String):Any = {
    logger.info("Executing " + operation + " with statement: " + sql)
    operation match {
      case SqlOperation.SELECT => select(sql)
      case _ => update(sql)
    }
  }

  private def select(sql:String):Any = {
    try {
      val rs = statement.executeQuery(sql)
      return rs
    } catch {
      case e: Exception => {
        println("Error while executing sql: " + sql)
        e.printStackTrace()
      }
    }
    return null
  }

  private def update(sql:String):Any = {
    try {
      //println("Executing statement: " + sql)
      val rs = statement.executeUpdate(sql)
      return rs
    } catch {
      case e: Exception => {
        println("Error while executing sql: " + sql)
        e.printStackTrace()
      }
    }
    return null
  }

  private def delete(sql:String):Any = update(sql)
  private def insert(sql:String):Any = update(sql)
}
