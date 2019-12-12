package com.concordia.blockchain.constants

object MessageType extends Enumeration {
  type MessageType = Value
  val REQUEST = Value("REQUEST")
  val RESPONSE = Value("RESPONSE")
  val INFO = Value("INFO")
}