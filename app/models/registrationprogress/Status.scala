package models.registrationprogress

sealed trait Status
case object NotStarted extends Status
case object Started extends Status
case object Completed extends Status
