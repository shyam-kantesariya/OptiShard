package com.concordia.blockchain.main

import com.concordia.blockchain.model.node.{Manager, ManagerLeader, Worker}
import com.concordia.blockchain.constants.{Constants, Role}

object LaunchApp {
  def main(args:Array[String]): Unit ={
    val managerLeader = new ManagerLeader(Role.CORE_LEADER, Constants.UDP_LOCAL_HOST, 12345)
    new Thread(managerLeader).start()
    val manager = new Manager(Role.CORE_FOLLOWER, Constants.UDP_LOCAL_HOST, 12346)
    new Thread(manager).start()
    val worker = new Worker(Role.NON_CORE_FOLLOWER, Constants.UDP_LOCAL_HOST, 12348)
    new Thread(worker).start()
  }
}