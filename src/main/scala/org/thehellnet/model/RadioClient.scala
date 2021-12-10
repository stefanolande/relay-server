package org.thehellnet.model

import java.net.InetAddress
import java.time.LocalDateTime

case class RadioClient(ip: InetAddress, port: Int, receivedAt: LocalDateTime)
