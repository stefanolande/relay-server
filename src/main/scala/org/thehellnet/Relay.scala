package org.thehellnet

import cats.effect.*
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent.*
import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime
import org.thehellnet.network.socket.SocketConnection
import org.thehellnet.network.{AudioChannel, RadioClientChannel}
import org.thehellnet.routes.Routes
import org.thehellnet.service.{ClientRegistrationService, CryptoService, RelayService}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.*
import pureconfig.generic.derivation.default.*

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

    resources.use { case (config, clientsSocket, radioSocket) =>
      val clientSocketConnection = new SocketConnection(clientsSocket, config.relayServer.udpPacketSize)
      val radioSocketConnection  = new SocketConnection(radioSocket, config.relayServer.udpPacketSize)

      val cryptoService =
        new CryptoService(encryptionKey = config.relayServer.security.encryptionKey, salt = config.relayServer.security.encryptionSalt)

      val audioChannel       = new AudioChannel(radioSocketConnection)
      val radioClientChannel = new RadioClientChannel(clientSocketConnection, cryptoService)

      for {
        _        <- logger.info(s"Starting relay with configuration:\n ${config.asJson}")
        clientsR <- Ref.of[IO, Map[RadioClient, ClientUpdateTime]](Map.empty)

        clientRegistrationService = new ClientRegistrationService(
          radioClientChannel,
          clientsR,
          config.relayServer.clientTTL,
          config.relayServer.clientExpirationCheck,
          config.relayServer.security.pingSecret
        )
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

  private def webServer(routes: Routes, host: String, port: Int): IO[Nothing] = {

    def httpApp: HttpApp[IO] =
      Router(
        "api" -> routes.routes,
        "/"   -> fileService[IO](FileService.Config("static/"))
      ).orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(port, host)
      .withHttpApp(httpApp)
      .resource
      .useForever
  }
}
