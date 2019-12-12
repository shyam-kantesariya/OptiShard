package com.concordia.blockchain.network

import com.concordia.blockchain.constants.MessageTag.MessageTag
import com.concordia.blockchain.constants.MessageType.MessageType

@SerialVersionUID(1)
case class Message(val messageType: MessageType, val messageTag: MessageTag, val message:Array[Byte])  extends
  Serializable
//https://stackoverflow.com/questions/1931466/sending-an-object-over-the-internet
