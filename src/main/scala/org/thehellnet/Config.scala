package org.thehellnet

case class Web(url: String, port: Int)

case class RelayServer(
    clientsPort: Int,
    audioPort: Int,
    udpPacketSize: Int,
    clientTTL: Int,
    clientExpirationCheck: Int,
    security: Security
)

case class ServiceConf(
    web: Web,
    relayServer: RelayServer
)

case class Security(
    encryptionKey: String,
    encryptionSalt: String,
    pingSecret: String
)
