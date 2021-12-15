package org.thehellnet.routes
import cats.effect.IO
import org.http4s.HttpRoutes
import org.thehellnet.routes.model.RadioClientsView
import org.thehellnet.service.ClientRegistrationService
import sttp.model.sse.ServerSentEvent
import sttp.tapir.server.http4s.Http4sServerInterpreter

class Routes(clientRegistrationService: ClientRegistrationService) {

  import Endpoints._
  import io.circe.generic.auto._
  import io.circe.syntax._

  private val sseRoute: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(sseEndpoint.serverLogicSuccess[IO] { _ =>
      IO(
        fs2.Stream
          .eval(clientRegistrationService.getActiveClients)
          .map { clientMap =>
            val view = RadioClientsView.fromModel(clientMap)
            ServerSentEvent(Some(view.asJson.toString()))
          }
      )
    })

  val routes: HttpRoutes[IO] = sseRoute
}
