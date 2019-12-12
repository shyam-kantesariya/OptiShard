package com.concordia.blockchain.constants
import scala.math.ceil

object Constants {
  val UDP_SERVER_PORT = 12345
  val UDP_PACKET_SIZE = 65000
  val UDP_LOCAL_HOST = "localhost"
  val MYSQL_CONN_STRING = "jdbc:mysql://localhost:3306/blockchain"
  val MYSQL_USER_ID = "blockchain_user"
  val MYSQL_PASSWORD = "blockchain"
  val MYSQL_DRIVER = "com.mysql.jdbc.Driver"
  val COMMITTEE_SIZE = 1
  val CONSENSUS_MAJORITY = ceil(COMMITTEE_SIZE/3) + 1
  //val CONSENSUS_MAJORITY = 2

  val KEY_FILE_PATH = "/home/ubuntu/keys"
  val LOG_DIR = "/home/ubuntu/logs"

  //val KEY_FILE_PATH = "/home/s_kante/research/code/research/scala/keys"
  //val LOG_DIR = "/home/s_kante/research/code/research/scala/logs"

  //val KEY_FILE_PATH = "D:\\blockchain\\keys"
  //val LOG_DIR = "D:\\blockchain\\logs"

  val MAX_UDP_REQUEST_IN_HISTORY = 100
  val MAX_TX_READ = 100
  val MIN_TX_TO_OFFLOAD = 5
}