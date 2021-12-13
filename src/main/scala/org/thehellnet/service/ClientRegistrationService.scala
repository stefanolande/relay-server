package org.thehellnet.service

import cats.effect.{IO, Ref}
import org.thehellnet.model.RadioClient
import org.thehellnet.network.RadioClientChannel
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class ClientRegistrationService(radioClientChannel: RadioClientChannel) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receiveClient(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] =
    for {
      client <- radioClientChannel.receive()
      _      <- logger.info(s"received client $client")
      _      <- clientsR.getAndUpdate(_ + client)
      _      <- receiveClient(clientsR)
    } yield ()

  def expireClients(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] =
    for {
      activeClients <- clientsR.modify { list =>
        val now               = LocalDateTime.now
        val notExpiredClients = list.filter(_.receivedAt.plus(10, ChronoUnit.SECONDS).isAfter(now))
        (notExpiredClients, list)
      }
      _ <- logger.info(s"active clients ${activeClients.mkString("[", ",", "]")}")
      _ <- IO.sleep(FiniteDuration(1, TimeUnit.SECONDS))
      _ <- expireClients(clientsR)
    } yield ()

}
