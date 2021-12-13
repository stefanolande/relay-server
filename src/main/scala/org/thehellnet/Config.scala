package org.thehellnet

object Config {

  val CLIENTS_PORT = 64123
  val AUDIO_PORT   = 1235
  val PACKET_SIZE  = 8192

  val CLIENT_TTL              = 10 // seconds
  val CLIENT_EXPIRATION_CHECK = 1 // every x seconds
}
