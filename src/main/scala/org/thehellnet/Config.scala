package org.thehellnet

case class Web(url: String, port: Int)

case class Relay(
    clientsPort: Int,
    audioPort: Int,
    udpPacketSize: Int,
    clientTTL: Int,
    clientExpirationCheck: Int
)

case class ServiceConf(
    web: Web,
    relay: Relay
)
