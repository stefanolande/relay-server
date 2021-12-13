package org.thehellnet.model.valueclass

case class Port(value: Int) extends AnyVal {
  override def toString: String = this.value.toString
}
