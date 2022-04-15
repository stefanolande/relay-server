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
import java.util
import java.util.UUID
import scala.util.Try

class RadioClientChannel(socketConnection: SocketConnection, crypto: CryptoService) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receive(): IO[Option[RadioClient]] = {
    val client = for {
      packet  <- OptionT.liftF(socketConnection.receive())
      _       <- OptionT.liftF(logger.debug(s"received ping packet"))
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

  private def parseMessage(packet: DatagramPacket): Option[RadioClient] = {

    val bytes = util.Arrays.copyOfRange(packet.getData, 0, packet.getLength)

    crypto
      .decrypt(bytes)
      .flatMap { message =>
        Try {
          val messageParts = message.split("\\|")

          RadioClient(
            secret     = messageParts(0),
            identifier = UUID.fromString(messageParts(1)),
            timestamp  = Instant.ofEpochSecond(messageParts(2).toLong),
            ip         = packet.getAddress,
            port       = Port(packet.getPort)
          )

        }
      }
      .toOption
  }
}
