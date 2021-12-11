package org.thehellnet.network

import cats.effect.IO
import org.thehellnet.Config.PACKET_SIZE
import org.thehellnet.model.{AudioData, RadioClient}
import org.thehellnet.network.socket.SocketConnection
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.DatagramPacket
import java.time.LocalDateTime

class RadioClientConnection(socketConnection: SocketConnection) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receive(): IO[RadioClient] =
    for {
      packet <- socketConnection.receive()
      client = RadioClient(packet.getAddress, packet.getPort, LocalDateTime.now)
    } yield client

  def forward(audioData: AudioData, radioClient: RadioClient): IO[Unit] = {
    val clientPacket = new DatagramPacket(audioData.payload, PACKET_SIZE, radioClient.ip, radioClient.port)
    socketConnection.send(clientPacket) >>
    logger.info(s"Forwarded audio packet to $radioClient")
  }
}
