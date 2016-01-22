package models.estateagentbusiness

sealed trait Service

object Service {

  case object Commercial extends Service
  case object Auction extends Service
  case object Relocation extends Service
  case object BusinessTransfer extends Service
  case object AssetManagement extends Service
  case object LandManagement extends Service
  case object Development extends Service
  case object SocialHousing extends Service
  case class Residential(redressScheme: Option[RedressScheme]) extends Service
}

