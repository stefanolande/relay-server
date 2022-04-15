package org.thehellnet.service

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

  private val PingExpirationSeconds = 15

  def getActiveClients: IO[Map[RadioClient, ClientUpdateTime]] = clientsR.get

  def clientsRegistrationLogic: IO[Nothing] =
    (
      this.expireClients,
      this.receiveClient
    ).parTupled.background.useForever

  private def receiveClient: IO[Unit] =
    IO.defer {
      for {
        maybeClient <- radioClientChannel.receive()
        _ <- if (maybeClient.isDefined) {
          for {
            _          <- logger.debug(s"received client ${maybeClient.get}")
            nowInstant <- Clock[IO].realTimeInstant
            _          <- clientsR.getAndUpdate(handleNewClient(maybeClient.get, _, nowInstant))

          } yield ()
        } else IO.unit
        _ <- receiveClient
      } yield ()
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

  private def validatePing(newClient: RadioClient, nowInstant: Instant): Boolean = {
    val timeDifference = ChronoUnit.SECONDS.between(nowInstant, newClient.timestamp)

    newClient.secret == pingSecret && timeDifference < PingExpirationSeconds
  }

  private def handleNewClient(newClient: RadioClient,
                              clientsMap: Map[RadioClient, ClientUpdateTime],
                              nowInstant: Instant): Map[RadioClient, ClientUpdateTime] = {
    val now = LocalDateTime.ofInstant(nowInstant, ZoneOffset.UTC)

    val clientUpdateTime = ClientUpdateTime(now)

    if (validatePing(newClient, nowInstant))
      clientsMap.filterNot { case (client, _) => client.isTheSameAs(newClient) } + (newClient -> clientUpdateTime)
    else
      clientsMap
  }

}
