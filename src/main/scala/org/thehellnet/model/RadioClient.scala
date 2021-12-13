package org.thehellnet.model

import org.thehellnet.model.valueclass.Port

import java.net.InetAddress

case class RadioClient(ip: InetAddress, port: Port)
