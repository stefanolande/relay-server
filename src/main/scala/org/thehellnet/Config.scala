package org.thehellnet

case class Web(url: String, port: Int)

case class RelayServer(
    clientsPort: Int,
    audioPort: Int,
    udpPacketSize: Int,
    clientTTL: Int,
    clientExpirationCheck: Int
)

case class ServiceConf(
    web: Web,
    relayServer: RelayServer
)
