package com.concordia.blockchain.model.udp

import scala.collection.mutable.ListBuffer

class RequestRegister {
  val pendingRequests:ListBuffer[RequestHandler] = ListBuffer[RequestHandler]()
  val processedRequests:ListBuffer[RequestHandler] = ListBuffer[RequestHandler]()

  def addToPendingList(requestHandler: RequestHandler) = {
    pendingRequests += requestHandler
  }

  def markAsProcessed(requestHandler: RequestHandler) = {
    processedRequests += requestHandler
    pendingRequests -= requestHandler
    if(processedRequests.length == 100) {
      processedRequests.clear
    }
  }

  def isAnyRequestPending:Boolean = {
    !pendingRequests.isEmpty
  }

  def getPendingRequestCnt:Int = {
    pendingRequests.length
  }
}