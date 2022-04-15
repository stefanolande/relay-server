package org.thehellnet.network

import cats.effect.IO
import org.thehellnet.model.AudioData
import org.thehellnet.network.socket.SocketConnection

import java.util

class AudioChannel(socketConnection: SocketConnection) {

  def receive(): IO[AudioData] =
    for {
      packet <- socketConnection.receive()
      bytes = util.Arrays.copyOfRange(packet.getData, 0, packet.getLength)
      data  = AudioData(bytes)
    } yield data
}
