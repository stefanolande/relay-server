package org.thehellnet.service

import cats.data.OptionT
import cats.effect.{Clock, IO, Ref}
import cats.implicits._
import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime
import org.thehellnet.network.RadioClientChannel
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class ClientRegistrationService(radioClientChannel: RadioClientChannel,
                                clientsR: Ref[IO, Map[RadioClient, ClientUpdateTime]],
                                clientTTL: Int,
                                clientExpirationCheck: Int,
                                pingSecret: String) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  private val PingExpirationMinutes = 1

  def getActiveClients: IO[Map[RadioClient, ClientUpdateTime]] = clientsR.get

  def clientsRegistrationLogic: IO[Nothing] =
    (
      this.expireClients,
      this.receiveClient
    ).parTupled.background.useForever

  private def receiveClient: IO[Unit] =
    IO.defer {
      val receiveClientT = for {
        client     <- OptionT(radioClientChannel.receive())
        _          <- OptionT.liftF(logger.debug(s"received client $client"))
        nowInstant <- OptionT.liftF(Clock[IO].realTimeInstant)
        _          <- OptionT.liftF(clientsR.getAndUpdate(handleNewClient(client, _, nowInstant)))
      } yield ()

      receiveClientT.value >> receiveClient
    }

  private def expireClients: IO[Unit] =
    IO.defer {
      for {
        nowInstant <- Clock[IO].realTimeInstant
        activeClients <- clientsR.modify { clientsMap =>
          val notExpiredClients = clientsMap.filter(isAlive(_, nowInstant))
          (notExpiredClients, clientsMap)
        }
        _ <- logger.debug(s"active clients ${activeClients.keySet.mkString("[", ",", "]")}")
        _ <- IO.sleep(FiniteDuration(clientExpirationCheck.toLong, TimeUnit.SECONDS))
        _ <- expireClients
      } yield ()
    }

  private def isAlive(client: (RadioClient, ClientUpdateTime), nowInstant: Instant): Boolean =
    client match {
      case (_, receivedAt) =>
        val now = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC)
        receivedAt.value.plus(clientTTL.toLong, ChronoUnit.SECONDS).isAfter(now)
    }

  private def handleNewClient(newClient: RadioClient,
                              clientsMap: Map[RadioClient, ClientUpdateTime],
                              nowInstant: Instant): Map[RadioClient, ClientUpdateTime] = {
    val currentInstant   = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC)
    val clientUpdateTime = ClientUpdateTime(currentInstant)

    val timeDifference = ChronoUnit.SECONDS.between(currentInstant, newClient.timestamp)

    if (newClient.secret == pingSecret && timeDifference < PingExpirationMinutes)
      clientsMap.filterNot { case (client, _) => client == newClient } + (newClient -> clientUpdateTime)
    else
      clientsMap
  }

}
