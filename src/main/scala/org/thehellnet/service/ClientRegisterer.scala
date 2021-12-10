package org.thehellnet.service

import cats.effect.{IO, Ref}
import org.thehellnet.Config
import org.thehellnet.model.RadioClient
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.{DatagramPacket, DatagramSocket}
import java.time.LocalDateTime

class ClientRegisterer(socket: DatagramSocket) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receiveClient(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] = {
    val buffer = new Array[Byte](Config.SIZE)
    val packet = new DatagramPacket(buffer, buffer.length)

    for {
      _ <- IO(socket.receive(packet))
      _      = logger.info(s"received from ${packet.getAddress}")
      client = RadioClient(packet.getAddress, LocalDateTime.now)
      _ <- clientsR.getAndUpdate(_ + client)
      _ <- receiveClient(clientsR)
    } yield ()
  }

}
