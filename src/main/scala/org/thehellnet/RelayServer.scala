package org.thehellnet

import cats.effect._
import cats.syntax.all._
import org.thehellnet.model.RadioClient
import org.thehellnet.network.{AudioChannel, RadioClientChannel}
import org.thehellnet.network.socket.SocketConnection
import org.thehellnet.service.{ClientRegistrationService, RelayService}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.net.DatagramSocket

object RelayServer extends IOApp.Simple {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  val run: IO[Unit] = {

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

        val clientRegistrationService = new ClientRegistrationService(radioClientChannel)
        val relayService              = new RelayService(audioChannel, radioClientChannel)

        for {
          clientsR <- Ref.of[IO, Set[RadioClient]](Set.empty)
          res <- (clientRegistrationService.expireClients(clientsR),
                  clientRegistrationService.receiveClient(clientsR),
                  relayService.forwardPacket(clientsR))
            .parMapN((_, _, _) => ExitCode.Success)
            .handleErrorWith { t =>
              logger.error(s"Error caught: ${t.getMessage}").as(ExitCode.Error)
            }
        } yield res
    }
  }

}
