package org.thehellnet
import pureconfig.*
import pureconfig.generic.derivation.default.*

case class Web(url: String, port: Int)

case class RelayServer(
    clientsPort: Int,
    audioPort: Int,
    udpPacketSize: Int,
    clientTTL: Int,
    clientExpirationCheck: Int,
    security: Security
)

case class Security(
    encryptionKey: String,
    encryptionSalt: String,
    pingSecret: String
)

case class ServiceConf(
    web: Web,
    relayServer: RelayServer
) derives ConfigReader
