package com.concordia.blockchain.constants

object SqlOperation extends Enumeration {
  type SqlOperation = Value
  val SELECT = Value("SELECT")
  val UPDATE = Value("UPDATE")
  val DELETE = Value("DELETE")
  val INSERT = Value("INSERT")
}