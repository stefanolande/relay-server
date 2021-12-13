package org.thehellnet.service

import cats.effect.{IO, Ref}
import org.thehellnet.Config
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
      _      <- clientsR.getAndUpdate(addOrUpdateClients(client, _))
      _      <- receiveClient(clientsR)
    } yield ()

  def expireClients(clientsR: Ref[IO, Set[RadioClient]]): IO[Unit] =
    for {
      activeClients <- clientsR.modify { clientsSet =>
        val notExpiredClients = clientsSet.filter(isAlive)
        (notExpiredClients, clientsSet)
      }
      _ <- logger.info(s"active clients ${activeClients.mkString("[", ",", "]")}")
      _ <- IO.sleep(FiniteDuration(1, TimeUnit.SECONDS))
      _ <- expireClients(clientsR)
    } yield ()

  private def isAlive(radioClient: RadioClient): Boolean = {
    val now = LocalDateTime.now
    radioClient.receivedAt.plus(Config.CLIENT_TTL, ChronoUnit.SECONDS).isAfter(now)
  }

  private def addOrUpdateClients(newClient: RadioClient, clientsSet: Set[RadioClient]): Set[RadioClient] =
    clientsSet.filterNot { registeredClient =>
      (registeredClient.ip, registeredClient.port) == (newClient.ip, newClient.port)
    } + newClient

}
