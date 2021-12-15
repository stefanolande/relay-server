package org.thehellnet.network

import cats.effect.IO
import org.thehellnet.model.valueclass.Port
import org.thehellnet.model.{AudioData, RadioClient}
import org.thehellnet.network.socket.SocketConnection
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.DatagramPacket

class RadioClientChannel(socketConnection: SocketConnection, packetSize: Int) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receive(): IO[RadioClient] =
    for {
      packet <- socketConnection.receive()
      client = RadioClient(packet.getAddress, Port(packet.getPort))
    } yield client

  def forward(audioData: AudioData, radioClient: RadioClient): IO[Unit] =
    for {
      clientPacket <- IO(new DatagramPacket(audioData.payload, packetSize, radioClient.ip, radioClient.port.value))
      _            <- socketConnection.send(clientPacket)
      _            <- logger.info(s"Forwarded audio packet to $radioClient")
    } yield ()
}
