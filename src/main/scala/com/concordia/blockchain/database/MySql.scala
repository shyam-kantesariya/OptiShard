package com.concordia.blockchain.database

import java.sql.{Connection, DriverManager, Statement}
import com.concordia.blockchain.constants.Constants._;

class MySql (private val url:String) {
  def this() = this(MYSQL_CONN_STRING)
  private val driver = "com.mysql.jdbc.Driver"
  private val username = MYSQL_USER_ID
  private val password = MYSQL_PASSWORD
  private var connection:Connection = _
  private var statement:Statement = _
  private var connEstablished = false

  def openConnection:Int = {
    if (connEstablished)
      return 1
    try {
      Class.forName(driver).newInstance
      connection = DriverManager.getConnection(url, username, password)
      statement = connection.createStatement
      connEstablished = true
      return 1
    } catch {
      case e: Exception => e.printStackTrace
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

  def executeQuery(operation:String, sql:String):Any = {
    operation match {
      case "select" => select(sql)
      case _ => update(sql)
    }
  }

  private def select(sql:String):Any = {
    try {
      val rs = statement.executeQuery(sql)
      return rs
    } catch {
      case e: Exception => {
        println("Error while reading data")
        e.printStackTrace()
      }
    }
    return null
  }

  private def update(sql:String):Any = {
    try {
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
