package org.thehellnet.model

import java.net.InetAddress
import java.time.LocalDateTime

case class RadioClient(ip: InetAddress, receivedAt: LocalDateTime)
