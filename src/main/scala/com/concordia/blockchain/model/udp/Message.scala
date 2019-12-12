package com.concordia.blockchain.model.udp

import java.security.PrivateKey

import com.concordia.blockchain.constants.MessageTag.MessageTag
import com.concordia.blockchain.constants.MessageType.MessageType
import com.concordia.blockchain.constants.Role.Role

@SerialVersionUID(1)
case class Message(val role:Role, val messageType: MessageType, val messageTag: MessageTag, val message:Any, val signature:Array[Byte])  extends
  Serializable {
  def this(role: Role, messageType: MessageType, messageTag: MessageTag) =
    this(role:Role, messageType: MessageType, messageTag: MessageTag, null, null)
}

//https://stackoverflow.com/questions/1931466/sending-an-object-over-the-internet
