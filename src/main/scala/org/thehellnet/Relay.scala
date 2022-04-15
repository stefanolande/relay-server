package org.thehellnet

import cats.effect._
import cats.syntax.all._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent._
import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime
import org.thehellnet.network.{AudioChannel, RadioClientChannel}
import org.thehellnet.network.socket.SocketConnection
import org.thehellnet.routes.Routes
import org.thehellnet.service.{ClientRegistrationService, RelayService}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import java.net.DatagramSocket

object Relay extends IOApp.Simple {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  override def run: IO[Unit] = {

    val resources = for {
      config <- Resource.eval(ConfigSource.default.load[ServiceConf] match {
        case Left(error)   => IO.raiseError(new RuntimeException(error.prettyPrint()))
        case Right(config) => IO.pure(config)
      })
      clientsSocketR <- Resource.fromAutoCloseable(IO(new DatagramSocket(config.relayServer.clientsPort)))
      radioSocketR   <- Resource.fromAutoCloseable(IO(new DatagramSocket(config.relayServer.audioPort)))
    } yield (config, clientsSocketR, radioSocketR)

    resources.use {
      case (config, clientsSocket, radioSocket) =>
        val clientSocketConnection = new SocketConnection(clientsSocket, config.relayServer.udpPacketSize)
        val radioSocketConnection  = new SocketConnection(radioSocket, config.relayServer.udpPacketSize)

        val audioChannel       = new AudioChannel(radioSocketConnection)
        val radioClientChannel = new RadioClientChannel(clientSocketConnection)

        for {
          clientsR <- Ref.of[IO, Map[RadioClient, ClientUpdateTime]](Map.empty)

          clientRegistrationService = new ClientRegistrationService(radioClientChannel,
                                                                    clientsR,
                                                                    config.relayServer.clientTTL,
                                                                    config.relayServer.clientExpirationCheck)
          relayService = new RelayService(audioChannel, radioClientChannel, clientsR)

          routes = new Routes(clientRegistrationService)

          _ <- (
            clientRegistrationService.clientsRegistrationLogic,
            relayService.forwardPackets,
            webServer(routes, config.web.url, config.web.port)
          ).parTupled.handleErrorWith { t =>
            logger.error(t)(s"Error caught: ${t.getMessage}")
          }
        } yield ()
    }
  }

  private def webServer(routes: Routes, host: String, port: Int): IO[Nothing] =
    BlazeServerBuilder[IO]
      .bindHttp(port, host)
      .withHttpApp(
        Router(
          "api" -> routes.routes,
          "/"   -> fileService[IO](FileService.Config("static/"))
        ).orNotFound
      )
      .resource
      .useForever
}
