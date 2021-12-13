package org.thehellnet.service

import cats.effect.{IO, Ref}
import org.thehellnet.Config
import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime
import org.thehellnet.network.RadioClientChannel
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class ClientRegistrationService(radioClientChannel: RadioClientChannel) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def receiveClient(clientsR: Ref[IO, Map[RadioClient, ClientUpdateTime]]): IO[Unit] =
    for {
      client <- radioClientChannel.receive()
      _      <- logger.info(s"received client $client")
      _      <- clientsR.getAndUpdate(addOrUpdateClients(client, _))
      _      <- receiveClient(clientsR)
    } yield ()

  def expireClients(clientsR: Ref[IO, Map[RadioClient, ClientUpdateTime]]): IO[Unit] =
    for {
      activeClients <- clientsR.modify { clientsMap =>
        val notExpiredClients = clientsMap.filter(isAlive)
        (notExpiredClients, clientsMap)
      }
      _ <- logger.info(s"active clients ${activeClients.keySet.mkString("[", ",", "]")}")
      _ <- IO.sleep(FiniteDuration(Config.CLIENT_EXPIRATION_CHECK, TimeUnit.SECONDS))
      _ <- expireClients(clientsR)
    } yield ()

  private def isAlive(client: (RadioClient, ClientUpdateTime)): Boolean = client match {
    case (_, receivedAt) =>
      val now = LocalDateTime.now
      receivedAt.value.plus(Config.CLIENT_TTL, ChronoUnit.SECONDS).isAfter(now)
  }

  private def addOrUpdateClients(newClient: RadioClient,
                                 clientsMap: Map[RadioClient, ClientUpdateTime]): Map[RadioClient, ClientUpdateTime] = {

    val now = ClientUpdateTime(LocalDateTime.now)
    clientsMap.filterNot { case (client, _) => client == newClient } + (newClient -> now)
  }

}
