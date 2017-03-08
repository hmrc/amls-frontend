package models.tradingpremises

import models.{DateOfChange, FormTypes}
import org.joda.time.LocalDate
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import play.api.libs.json.{Reads, Writes}
import utils.MappingUtils.Implicits._

case class YourTradingPremises(
                                tradingName: String,
                                tradingPremisesAddress: Address,
                                isResidential: Option[Boolean],
                                startDate: Option[LocalDate],
                                tradingNameChangeDate: Option[DateOfChange] = None
                              )

object YourTradingPremises {

  val maxLengthPremisesTradingName = 120
  val premisesTradingNameType = FormTypes.notEmptyStrip andThen
    notEmpty.withMessage("error.required.tp.trading.name") andThen
    maxLength(maxLengthPremisesTradingName).withMessage("error.invalid.tp.trading.name") andThen
    FormTypes.basicPunctuationPattern()

  implicit val reads: Reads[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").read[String] and
        __.read[Address] and
        (__ \ "isResidential").readNullable[Boolean] and
        (__ \ "startDate").readNullable[LocalDate] and
        (__ \ "tradingNameChangeDate").readNullable[DateOfChange]
      ) (YourTradingPremises.apply _)
  }

  implicit val writes: Writes[YourTradingPremises] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "tradingName").write[String] and
        __.write[Address] and
        (__ \ "isResidential").writeNullable[Boolean] and
        (__ \ "startDate").writeNullable[LocalDate] and
        (__ \ "tradingNameChangeDate").writeNullable[DateOfChange]
      ) (unlift(YourTradingPremises.unapply))
  }

  implicit val formR: Rule[UrlFormEncoded, YourTradingPremises] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import jto.validation.forms.Rules._
      (
        (__ \ "tradingName").read(premisesTradingNameType) ~
          __.read[Address] ~
          (__ \ "isResidential").read[Option[Boolean]] ~
          (__ \ "startDate").read(optionR(localDateRule)) ~
          (__ \ "tradingNameChangeDate").read[Option[DateOfChange]]
        ) (YourTradingPremises.apply)
    }

  implicit val formW: Write[YourTradingPremises, UrlFormEncoded] =
    To[UrlFormEncoded] { __ =>
      import models.FormTypes.localDateWrite
      import jto.validation.forms.Writes._
      import play.api.libs.functional.syntax.unlift
      (
        (__ \ "tradingName").write[String] ~
          __.write[Address] ~
          (__ \ "isResidential").write[Option[Boolean]] ~
          (__ \ "startDate").write(optionW(localDateWrite)) ~
          (__ \ "tradingNameChangeDate").write[Option[DateOfChange]]
        ) (unlift(YourTradingPremises.unapply))
    }

  implicit def convert(data: YourTradingPremises): Option[TradingPremises] = {
    Some(TradingPremises(yourTradingPremises = Some(data)))
  }
}
