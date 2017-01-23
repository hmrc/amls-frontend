package models.tradingpremises

import models.{DateOfChange, FormTypes}
import org.joda.time.LocalDate
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.libs.json.{Reads, Writes}
import utils.MappingUtils.Implicits._

case class YourTradingPremises(
                                tradingName: String,
                                tradingPremisesAddress: Address,
                                isResidential: Boolean,
                                startDate: LocalDate,
                                tradingNameChangeDate: Option[DateOfChange] = None
                              )

object YourTradingPremises {

  val maxLengthPremisesTradingName = 120
  val premisesTradingNameType = FormTypes.notEmptyStrip.withMessage("error.required.tp.trading.name") compose
    maxLength(maxLengthPremisesTradingName).withMessage("error.invalid.tp.trading.name")

  implicit val reads: Reads[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").read[String] and
        __.read[Address] and
        (__ \ "isResidential").read[Boolean] and
        (__ \ "startDate").read[LocalDate] and
        (__ \ "tradingNameChangeDate").readNullable[DateOfChange]
      ) (YourTradingPremises.apply _)
  }

  implicit val writes: Writes[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").write[String] and
        __.write[Address] and
        (__ \ "isResidential").write[Boolean] and
        (__ \ "startDate").write[LocalDate] and
        (__ \ "tradingNameChangeDate").writeNullable[DateOfChange]
      ) (unlift(YourTradingPremises.unapply))
  }

  implicit val formR: Rule[UrlFormEncoded, YourTradingPremises] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "tradingName").read(premisesTradingNameType) ~
          __.read[Address] ~
          (__ \ "isResidential").read[Boolean].withMessage("error.required.tp.residential.address") ~
          (__ \ "startDate").read(localDateRule)
        ) ((tradingName: String, address: Address, isResidential: Boolean, startDate: LocalDate) =>
        YourTradingPremises(tradingName, address, isResidential, startDate))
    }

  def unapplyWithoutDateOfChange(data: YourTradingPremises) = {
    Some((data.tradingName, data.tradingPremisesAddress,data.isResidential, data.startDate))
  }

  implicit val formW: Write[YourTradingPremises, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      import models.FormTypes.localDateWrite
      import play.api.data.mapping.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      (
        (__ \ "tradingName").write[String] ~
          __.write[Address] ~
          (__ \ "isResidential").write[Boolean] ~
          (__ \ "startDate").write(localDateWrite)
        ) (unlift(YourTradingPremises.unapplyWithoutDateOfChange))
    }

  implicit def convert(data: YourTradingPremises): Option[TradingPremises] = {
    Some(TradingPremises(yourTradingPremises = Some(data)))
  }
}
