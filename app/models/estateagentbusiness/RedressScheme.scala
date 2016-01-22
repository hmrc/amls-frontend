package models.estateagentbusiness

sealed trait RedressScheme

object RedressScheme {

  case object ThePropertyOmbudsman extends RedressScheme
  case object OmbudsmanServices extends RedressScheme
  case object PropertyRedressScheme extends RedressScheme
  case class Other(v: String) extends RedressScheme
}

