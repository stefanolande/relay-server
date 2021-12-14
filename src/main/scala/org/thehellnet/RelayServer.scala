package org.thehellnet

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.staticcontent._
import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime
import org.thehellnet.network.socket.SocketConnection
import org.thehellnet.network.{AudioChannel, RadioClientChannel}
import org.thehellnet.routes.Routes
import org.thehellnet.service.{ClientRegistrationService, RelayService}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.DatagramSocket
import scala.concurrent.ExecutionContext
object RelayServer extends IOApp {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  override def run(args: List[String]): IO[ExitCode] = {

    val resources = for {
      clientsSocketR <- Resource.fromAutoCloseable(IO(new DatagramSocket(Config.CLIENTS_PORT)))
      radioSocketR   <- Resource.fromAutoCloseable(IO(new DatagramSocket(Config.AUDIO_PORT)))
    } yield (clientsSocketR, radioSocketR)

    resources.use {
      case (clientsSocket, radioSocket) =>
        val clientSocketConnection = new SocketConnection(clientsSocket)
        val radioSocketConnection  = new SocketConnection(radioSocket)

        val audioChannel       = new AudioChannel(radioSocketConnection)
        val radioClientChannel = new RadioClientChannel(clientSocketConnection)

        for {
          clientsR <- Ref.of[IO, Map[RadioClient, ClientUpdateTime]](Map.empty)

          clientRegistrationService = new ClientRegistrationService(radioClientChannel, clientsR)
          relayService              = new RelayService(audioChannel, radioClientChannel, clientsR)

          routes = new Routes(clientRegistrationService)

          _ <- (clientRegistrationService.clientsRegistrationLogic, relayService.forwardPackets, webServer(routes)).parTupled.handleErrorWith {
            t =>
              logger.error(t)(s"Error caught: ${t.getMessage}").as(ExitCode.Error)
          }
        } yield ExitCode.Success
    }
  }

  private def webServer(routes: Routes): IO[Nothing] = {

    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    val httpApp: HttpApp[IO] =
      Router("api" -> routes.routes, "/" -> fileService[IO](FileService.Config("static/"))).orNotFound

    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .resource
      .useForever
  }
}
