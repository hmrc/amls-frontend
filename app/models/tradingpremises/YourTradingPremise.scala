package models.tradingpremises


import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.validation.ValidationError
import play.api.libs.json._

case class YourTradingPremises(name:String, city:String)

object YourTradingPremises {

  implicit val formRule: Rule[UrlFormEncoded, YourTradingPremises] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._
      (
        (__ \ "name").read[String] and
          (__ \ "city").read[String]
        )(YourTradingPremises.apply _)
    }

  implicit val formWrites: Write[YourTradingPremises, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "name").write[String] and
        (__ \ "city").write[String]
      ) (unlift(YourTradingPremises.unapply _))
  }

  implicit val formats = Json.format[YourTradingPremises]
}
/*
import org.joda.time.LocalDate

case class YourTradingPremises(tradingName : String,
                              tradingAddress: TradingPremisesAddress,
                              startOfTradingDate: LocalDate,
                              isResidential : IsResidential)

sealed trait TradingPremisesAddress {

  def toLines: Seq[String] = this match {
    case a: TradingPremisesAddressUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: TradingPremisesAddressNonUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.country)
      ).flatten
  }
}

case class TradingPremisesAddressUK(
                               addressLine1: String,
                               addressLine2: String,
                               addressLine3: Option[String] = None,
                               addressLine4: Option[String] = None,
                               postCode: String
                             ) extends TradingPremisesAddress

case class TradingPremisesAddressNonUK(
                                  addressLine1: String,
                                  addressLine2: String,
                                  addressLine3: Option[String] = None,
                                  addressLine4: Option[String] = None,
                                  country: String
                                ) extends TradingPremisesAddress

sealed trait IsResidential
case object ResidentialYes extends IsResidential
case object ResidentialNo extends IsResidential
*/