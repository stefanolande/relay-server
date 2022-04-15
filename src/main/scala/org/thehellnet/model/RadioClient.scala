package org.thehellnet.model

import org.thehellnet.model.valueclass.Port

import java.net.InetAddress
import java.time.Instant
import java.util.UUID

case class RadioClient(ip: InetAddress, port: Port, secret: String, identifier: UUID, timestamp: Instant) {

  def isTheSameAs(other: RadioClient): Boolean =
    this.ip == other.ip && this.port == other.port && this.identifier == other.identifier
}
