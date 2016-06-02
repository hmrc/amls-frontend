package models.moneyservicebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class fundsTransfer(transferWithoutFormalSystems : Boolean)

object fundsTransfer {

implicit val formats = Json.format[fundsTransfer]
import utils.MappingUtils.Implicits._

implicit val formRule: Rule[UrlFormEncoded, fundsTransfer] =
From[UrlFormEncoded] { __ =>
import play.api.data.mapping.forms.Rules._
(__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Boolean].withMessage("error.required.atb.confirm.office") fmap fundsTransfer.apply
}

implicit val formWrites: Write[fundsTransfer, UrlFormEncoded] =
Write {
case fundsTransfer(b) =>
Map("transferWithoutFormalSystems" -> Seq(b.toString))
}
}
