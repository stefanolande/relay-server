package org.thehellnet.service

import cats.effect.{IO, Ref}
import cats.implicits._
import org.thehellnet.Config
import org.thehellnet.Config.SIZE
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

  def expireClients(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] =
    for {
      iO <- clientsR.modify { list =>
        val now               = LocalDateTime.now
        val notExpiredClients = list.filter(_.receivedAt.plus(1, ChronoUnit.SECONDS).isAfter(now))
        (notExpiredClients, list)
      }
      _ <- logger.info(s"active clients $iO")
      _ <- IO.sleep(FiniteDuration(100, TimeUnit.MILLISECONDS))
      _ <- expireClients(clientsR)
    } yield ()

  def forwardPacket(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] = {
    val buffer = new Array[Byte](SIZE)
    val packet = new DatagramPacket(buffer, buffer.length)

    for {
      _ <- IO(radioSocket.receive(packet))
      _ = logger.info(s"received audio packet")
      clients <- clientsR.get
      _       <- clients.toList.map(forwardPacketToClient(_, packet)).sequence
      _       <- expireClients(clientsR)
    } yield ()
  }

  private def forwardPacketToClient(radioClient: RadioClient, packet: DatagramPacket): IO[Unit] = {
    val clientPacket = new DatagramPacket(packet.getData, SIZE, radioClient.ip, Config.AUDIO_PORT)
    IO(clientsSocket.send(clientPacket))
  }

}
