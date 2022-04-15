package org.thehellnet.network

import cats.data.OptionT
import cats.effect.IO
import org.thehellnet.model.valueclass.Port
import org.thehellnet.model.{AudioData, RadioClient}
import org.thehellnet.network.socket.SocketConnection
import org.thehellnet.service.CryptoService
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.DatagramPacket
import java.time.Instant
import java.util.UUID
import scala.util.Try

class RadioClientChannel(socketConnection: SocketConnection, crypto: CryptoService) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receive(): IO[Option[RadioClient]] = {
    val client = for {
      packet  <- OptionT.liftF(socketConnection.receive())
      clientT <- OptionT.fromOption[IO](parseMessage(packet))
    } yield clientT
    client.value
  }

  def forward(audioData: AudioData, radioClient: RadioClient): IO[Unit] =
    for {
      clientPacket <- IO(new DatagramPacket(audioData.payload, audioData.payload.length, radioClient.ip, radioClient.port.value))
      _            <- socketConnection.send(clientPacket)
      _            <- logger.debug(s"Forwarded audio packet of ${audioData.payload.length} bytes to $radioClient")
    } yield ()

  private def parseMessage(packet: DatagramPacket): Option[RadioClient] =
    crypto
      .decrypt(packet.getData)
      .flatMap { message =>
        Try {
          val messageParts = message.split("\\|")

          RadioClient(
            secret     = messageParts(0),
            identifier = UUID.fromString(messageParts(1)),
            timestamp  = Instant.parse(messageParts(1)),
            ip         = packet.getAddress,
            port       = Port(packet.getPort)
          )
        }
      }
      .toOption
}
