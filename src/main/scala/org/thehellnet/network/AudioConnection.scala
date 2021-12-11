package org.thehellnet.network

import cats.effect.IO
import org.thehellnet.model.AudioData
import org.thehellnet.network.socket.SocketConnection

class AudioConnection(socketConnection: SocketConnection) {

  def receive(): IO[AudioData] =
    for {
      packet <- socketConnection.receive()
      data = AudioData(packet.getData)
    } yield data
}
