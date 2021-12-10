package org.thehellnet

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import org.thehellnet.model.RadioClient
import org.thehellnet.service.{ClientRegistrationService, RelayService}

import java.net.DatagramSocket

object RelayServer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {

    val clientsSocket = new DatagramSocket(Config.CLIENTS_PORT)
    val radioSocket   = new DatagramSocket(Config.AUDIO_PORT)

    val clientRegistrationService = new ClientRegistrationService(clientsSocket)
    val relayService              = new RelayService(radioSocket, clientsSocket)

    for {
      clientsR <- Ref.of[IO, Set[RadioClient]](Set.empty)
      res <- (clientRegistrationService.expireClients(clientsR),
              clientRegistrationService.receiveClient(clientsR),
              relayService.forwardPacket(clientsR))
        .parMapN((_, _, _) => ExitCode.Success)
        .handleErrorWith { t =>
          Console[IO].errorln(s"Error caught: ${t.getMessage}").as(ExitCode.Error)
        }
    } yield res
  }

}
