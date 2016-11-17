package models.notifications

sealed trait MessageType

case object APA1 extends MessageType
case object APR1 extends MessageType

case object REJR extends MessageType
case object REVR extends MessageType
case object EXPR extends MessageType

case object RPA1 extends MessageType
case object RPV1 extends MessageType
case object RPR1 extends MessageType
case object RPM1 extends MessageType
case object RREM extends MessageType

case object MTRJ extends MessageType
case object MTRV extends MessageType
case object NMRJ extends MessageType
case object NMRV extends MessageType
case object OTHR extends MessageType