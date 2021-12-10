package org.thehellnet.service

import cats.effect.{IO, Ref}
import cats.implicits._
import org.thehellnet.Config
import org.thehellnet.Config.PACKET_SIZE
import org.thehellnet.model.RadioClient
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.{DatagramPacket, DatagramSocket}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class RelayService(radioSocket: DatagramSocket, clientsSocket: DatagramSocket) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def forwardPacket(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] = {
    val buffer = new Array[Byte](PACKET_SIZE)
    val packet = new DatagramPacket(buffer, buffer.length)

    for {
      _ <- IO(radioSocket.receive(packet))
      _ = logger.info(s"received audio packet")
      clients <- clientsR.get
      _       <- clients.toList.map(forwardPacketToClient(_, packet)).sequence
      _       <- forwardPacket(clientsR)
    } yield ()
  }

  private def forwardPacketToClient(radioClient: RadioClient, packet: DatagramPacket): IO[Unit] = {
    val clientPacket = new DatagramPacket(packet.getData, PACKET_SIZE, radioClient.ip, radioClient.port)
    IO(clientsSocket.send(clientPacket))
  }

}
