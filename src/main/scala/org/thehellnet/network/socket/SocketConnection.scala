package org.thehellnet.network.socket

import cats.effect.IO
import cats.implicits._
import org.thehellnet.Config

import java.net.{DatagramPacket, DatagramSocket}

class SocketConnection(socket: DatagramSocket) {

  def receive(): IO[DatagramPacket] = {
    val buffer = new Array[Byte](Config.PACKET_SIZE)
    val packet = new DatagramPacket(buffer, buffer.length)
    IO.interruptible(socket.receive(packet)) >> packet.pure[IO]
  }

  def send(datagramPacket: DatagramPacket): IO[Unit] =
    IO.blocking(socket.send(datagramPacket))

}
