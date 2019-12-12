package com.concordia.blockchain.constants

object Role extends Enumeration {

  type Role = Value
  val CORE_LEADER = Value(1,"CORE_LEADER")
  val CORE_FOLLOWER = Value(2,"CORE_FOLLOWER")
  val NON_CORE_LEADER = Value(3,"NON_CORE_LEADER")
  val NON_CORE_FOLLOWER = Value(4,"NON_CORE_FOLLOWER")

  def getRoleId(role: Role) = {
    role match {
      case Role.CORE_LEADER => {
        1
      }
      case Role.CORE_FOLLOWER => {
        2
      }
      case Role.NON_CORE_LEADER => {
        3
      }
      case Role.NON_CORE_FOLLOWER => {
        4
      }
      case _ => 5
    }
  }
}