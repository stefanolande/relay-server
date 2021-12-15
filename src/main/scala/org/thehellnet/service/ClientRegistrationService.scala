package org.thehellnet.service

import cats.effect.{Clock, IO, Ref}
import cats.implicits._
import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime
import org.thehellnet.network.RadioClientChannel
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class ClientRegistrationService(radioClientChannel: RadioClientChannel,
                                clientsR: Ref[IO, Map[RadioClient, ClientUpdateTime]],
                                clientTTL: Int,
                                clientExpirationCheck: Int) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def getActiveClients: IO[Map[RadioClient, ClientUpdateTime]] = clientsR.get

  def clientsRegistrationLogic: IO[Nothing] =
    (
      this.expireClients,
      this.receiveClient
    ).parTupled.background.useForever

  private def receiveClient: IO[Unit] =
    for {
      client     <- radioClientChannel.receive()
      _          <- logger.info(s"received client $client")
      nowInstant <- Clock[IO].realTimeInstant
      _          <- clientsR.getAndUpdate(addOrUpdateClients(client, _, nowInstant))
      _          <- IO.defer(receiveClient)
    } yield ()

  private def expireClients: IO[Unit] =
    for {
      nowInstant <- Clock[IO].realTimeInstant
      activeClients <- clientsR.modify { clientsMap =>
        val notExpiredClients = clientsMap.filter(isAlive(_, nowInstant))
        (notExpiredClients, clientsMap)
      }
      _ <- logger.info(s"active clients ${activeClients.keySet.mkString("[", ",", "]")}")
      _ <- IO.sleep(FiniteDuration(clientExpirationCheck.toLong, TimeUnit.SECONDS))
      _ <- IO.defer(expireClients)
    } yield ()

  private def isAlive(client: (RadioClient, ClientUpdateTime), nowInstant: Instant): Boolean =
    client match {
      case (_, receivedAt) =>
        val now = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC)
        receivedAt.value.plus(clientTTL.toLong, ChronoUnit.SECONDS).isAfter(now)
    }

  private def addOrUpdateClients(newClient: RadioClient,
                                 clientsMap: Map[RadioClient, ClientUpdateTime],
                                 nowInstant: Instant): Map[RadioClient, ClientUpdateTime] = {

    val now = ClientUpdateTime(LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC))
    clientsMap.filterNot { case (client, _) => client == newClient } + (newClient -> now)
  }

}
