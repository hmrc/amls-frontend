package models.tradingpremises

import org.joda.time.LocalDate
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.libs.json.{Reads, Writes}
import utils.MappingUtils.Implicits._

case class YourTradingPremises(
                                tradingName: String,
                                tradingPremisesAddress: Address,
                                isOwner: Boolean,
                                startDate: LocalDate,
                                isResidential: Boolean
                              )

object YourTradingPremises {

  val maxLengthPremisesTradingName = 120
  val premisesTradingNameType = notEmpty.withMessage("error.required.tp.trading.name") compose
    maxLength(maxLengthPremisesTradingName).withMessage("error.invalid.tp.trading.name")

  implicit val reads: Reads[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").read[String] and
        __.read[Address] and
        (__ \ "isOwner").read[Boolean] and
        (__ \ "startDate").read[LocalDate] and
        (__ \ "isResidential").read[Boolean]
      ) (YourTradingPremises.apply _)
  }

  implicit val writes: Writes[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").write[String] and
        __.write[Address] and
        (__ \ "isOwner").write[Boolean] and
        (__ \ "startDate").write[LocalDate] and
        (__ \ "isResidential").write[Boolean]
      ) (unlift(YourTradingPremises.unapply))
  }

  implicit val formR: Rule[UrlFormEncoded, YourTradingPremises] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "tradingName").read(premisesTradingNameType) ~
          __.read[Address] ~
          (__ \ "isOwner").read[Boolean].withMessage("error.required.tp.your.business.or.other") ~
          (__ \ "startDate").read(localDateRule) ~
          (__ \ "isResidential").read[Boolean].withMessage("error.required.tp.residential.address")
        ) (YourTradingPremises.apply _)
    }

  implicit val formW: Write[YourTradingPremises, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      import models.FormTypes.localDateWrite
      import play.api.data.mapping.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      (
        (__ \ "tradingName").write[String] ~
          __.write[Address] ~
          (__ \ "isOwner").write[Boolean] ~
          (__ \ "startDate").write(localDateWrite) ~
          (__ \ "isResidential").write[Boolean]
        ) (unlift(YourTradingPremises.unapply))
    }
}
