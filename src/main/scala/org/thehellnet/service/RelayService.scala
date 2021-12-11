package org.thehellnet.service

import cats.effect.{IO, Ref}
import cats.implicits._
import org.thehellnet.Config
import org.thehellnet.Config.PACKET_SIZE
import org.thehellnet.model.RadioClient
import org.thehellnet.network.{AudioChannel, RadioClientChannel}
import org.thehellnet.network.socket.SocketConnection
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.{DatagramPacket, DatagramSocket}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class RelayService(audioChannel: AudioChannel, radioClientChannel: RadioClientChannel) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def forwardPacket(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] =
    for {
      audioData <- audioChannel.receive()
      _         <- logger.info(s"received audio packet")
      clients   <- clientsR.get
      _         <- clients.toList.map(radioClientChannel.forward(audioData, _)).sequence
      _         <- forwardPacket(clientsR)
    } yield ()

}
