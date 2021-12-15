package org.thehellnet.service

import cats.effect.{IO, Ref}
import cats.implicits._
import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime
import org.thehellnet.network.{AudioChannel, RadioClientChannel}
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class RelayService(audioChannel: AudioChannel,
                   radioClientChannel: RadioClientChannel,
                   clientsR: Ref[IO, Map[RadioClient, ClientUpdateTime]]) {

  private val logger: StructuredLogger[IO] = Slf4jLogger.getLogger

  def forwardPackets: IO[Unit] =
    for {
      audioData <- audioChannel.receive()
      _         <- logger.info(s"received audio packet")
      clients   <- clientsR.get
      _         <- clients.keys.toList.map(radioClientChannel.forward(audioData, _)).sequence
      _         <- IO.defer(forwardPackets)
    } yield ()
}
