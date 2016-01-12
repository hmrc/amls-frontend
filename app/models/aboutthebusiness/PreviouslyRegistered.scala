package models.aboutthebusiness

sealed trait PreviouslyRegistered

case object PreviouslyRegisteredNo extends PreviouslyRegistered
case class PreviouslyRegisteredYes(value : String) extends PreviouslyRegistered

