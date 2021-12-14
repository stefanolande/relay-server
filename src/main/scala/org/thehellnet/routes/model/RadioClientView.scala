package org.thehellnet.routes.model

import org.thehellnet.model.RadioClient
import org.thehellnet.model.valueclass.ClientUpdateTime

import java.time.LocalDateTime

case class RadioClientView(ip: String, port: Int, lastSeen: LocalDateTime)

case class RadioClientsView(clients: List[RadioClientView])

object RadioClientsView {
  def fromModel(clients: Map[RadioClient, ClientUpdateTime]): RadioClientsView = {
    val list = clients.map {
      case (client, time) => RadioClientView(client.ip.toString, client.port.value, time.value)
    }.toList
    RadioClientsView(list)
  }
}
