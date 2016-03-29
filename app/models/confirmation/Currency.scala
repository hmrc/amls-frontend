package models.confirmation

case class Currency(value: BigDecimal) {

  override def toString: String =
    f"£${value.toInt}%,d"
}

object Currency {

  implicit def fromBD(value: BigDecimal): Currency =
    Currency(value)

  implicit def fromInt(value: Int): Currency =
    Currency(value)
}
