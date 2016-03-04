package models.businessactivities

import play.api.data.mapping.{Write, From, Rule}
import play.api.data.mapping.forms._
import play.api.libs.json.Json

case class AccountantForAMLSRegulations(accountantForAMLSRegulations: Boolean)


object AccountantForAMLSRegulations {

  implicit val formats = Json.format[AccountantForAMLSRegulations]

  implicit val formRule: Rule[UrlFormEncoded, AccountantForAMLSRegulations] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "accountantForAMLSRegulations").read[Boolean] fmap AccountantForAMLSRegulations.apply
  }

  implicit val formWrites: Write[AccountantForAMLSRegulations, UrlFormEncoded] = Write {
    case AccountantForAMLSRegulations(registered) => Map("accountantForAMLSRegulations" -> Seq(registered.toString))
  }

}