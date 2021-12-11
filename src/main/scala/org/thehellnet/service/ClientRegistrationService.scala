package org.thehellnet.service

import cats.effect.{IO, Ref}
import org.thehellnet.Config
import org.thehellnet.model.RadioClient
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.{DatagramPacket, DatagramSocket}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class ClientRegistrationService(socket: DatagramSocket) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receiveClient(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] = {
    val buffer = new Array[Byte](Config.PACKET_SIZE)
    val packet = new DatagramPacket(buffer, buffer.length)

    for {
      _ <- IO.blocking(socket.receive(packet))
      _      = logger.info(s"received from ${packet.getAddress}")
      client = RadioClient(packet.getAddress, packet.getPort, LocalDateTime.now)
      _ <- clientsR.getAndUpdate(_ + client)
      _ <- receiveClient(clientsR)
    } yield ()
  }

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

}
