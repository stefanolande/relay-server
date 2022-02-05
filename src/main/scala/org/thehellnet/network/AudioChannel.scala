package org.thehellnet.network

import cats.effect.IO
import org.thehellnet.model.AudioData
import org.thehellnet.network.socket.SocketConnection

class AudioChannel(socketConnection: SocketConnection) {

  def receive(): IO[AudioData] =
    for {
      packet <- socketConnection.receive()
      data = AudioData(packet.getData, packet.getLength)
    } yield data
}
