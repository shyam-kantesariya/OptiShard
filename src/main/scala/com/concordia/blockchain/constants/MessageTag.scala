package com.concordia.blockchain.constants

object MessageTag extends Enumeration {
  type MessageTag = Value
  val PROCESSED_TX_DIGEST = Value("PROCESSED_TX_DIGEST")
  val FINAL_AGG_PROCESSED_TX_DIGEST = Value("FINAL_AGG_PROCESSED_TX_DIGEST")
  val PROPOSE_TX_LIST_TO_BE_PROCESSED = Value("PROPOSE_TX_LIST_TO_BE_PROCESSED")
  val PROPOSE_TX_LIST_DIGEST_TO_BE_PROCESSED = Value("PROPOSE_TX_LIST_DIGEST_TO_BE_PROCESSED")
  val AGREED_TX_LIST_TO_BE_PROCESSED = Value("AGREED_TX_LIST_TO_BE_PROCESSED")
  val PROCESSED_TX_LIST = Value("PROCESSED_TX_LIST")
  val SHARD_DIGEST = Value("SHARD_DIGEST")
  val TX_LIST_TO_BE_PROCESSED = Value("TX_LIST_TO_BE_PROCESSED")
  val ASSIGN_WORKER_LEADER = Value("ASSIGN_WORKER_LEADER")
  val PUBLIC_KEY = Value("PUBLIC_KEY")
  val COMMITTEE_MEMBERS = Value("COMMITTEE_MEMBERS")
  val COMMITTEE_LEADER = Value("COMMITTEE_LEADER")
  val PING = Value("PING")
  val PARTNERS_ACROSS_COMMITTEES = Value("PARTNERS_ACROSS_COMMITTEES")
  val WORK_SHARING = Value("WORK_SHARING")
  val OFFER_HELP = Value("OFFER_HELP")
}