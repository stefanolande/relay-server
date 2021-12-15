package org.thehellnet.network.socket

import cats.effect.IO
import cats.implicits._

import java.net.{DatagramPacket, DatagramSocket}

class SocketConnection(socket: DatagramSocket, packetSize: Int) {

  def receive(): IO[DatagramPacket] = {
    val buffer = new Array[Byte](packetSize)
    val packet = new DatagramPacket(buffer, buffer.length)
    IO.interruptible(socket.receive(packet)) >> packet.pure[IO]
  }

  def send(datagramPacket: DatagramPacket): IO[Unit] =
    IO.blocking(socket.send(datagramPacket))

}
