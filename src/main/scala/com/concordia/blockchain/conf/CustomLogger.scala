package com.concordia.blockchain.conf
import java.io.IOException
import java.nio.file.Paths
import java.util.logging.{FileHandler, Level, Logger, SimpleFormatter}

import com.concordia.blockchain.constants.Constants
import com.concordia.blockchain.constants.Role.Role

object CustomLogger {

  @throws[IOException]
  def getLogger(ip:String, port:Int, role:Role): Logger = {
    val logger = Logger.getLogger(ip+"_"+port)
    val path = Paths.get(Constants.LOG_DIR, role.toString + "_" +ip + "_" + port+".log")
    val fHandler = new FileHandler(path.toString, true)
    fHandler.setFormatter(new SimpleFormatter)
    logger.addHandler(fHandler)
    logger.setLevel(Level.INFO)
    logger
  }
}
