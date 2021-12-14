package org.thehellnet.routes

import cats.effect.IO
import sttp.capabilities.fs2.Fs2Streams
import sttp.model.sse.ServerSentEvent
import sttp.tapir._
import sttp.tapir.server.http4s.serverSentEventsBody

object Endpoints {

  val sseEndpoint: Endpoint[Unit, Unit, Unit, fs2.Stream[IO, ServerSentEvent], Any with Fs2Streams[IO]] =
    endpoint.get.in("clients").out(serverSentEventsBody[IO])
}
