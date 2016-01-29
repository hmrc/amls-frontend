package models.tradingpremises

import org.joda.time.LocalDate
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{Success, Path, Rule}
import play.api.libs.functional.FunctionalBuilder
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.mapping.forms.Rules._
import play.api.libs.json.Writes._

case class YourTradingPremises(tradingName : String,
                              tradingAddress: TradingPremisesAddress,
                              startOfTradingDate: LocalDate,
                              isResidential : IsResidential)

object YourTradingPremises {
  implicit val jsonReadsYourTradingPremises = {
    ((JsPath \ "tradingName").read[String] and
      JsPath.read[TradingPremisesAddress] and
      (JsPath \ "startOfTrading").read[LocalDate] and
      (JsPath).read[IsResidential]) (YourTradingPremises.apply _)
  }

  implicit val jsonWritesYourTradingPremises: Writes[YourTradingPremises] = (
    (__ \ "tradingName").write[String] and
      (__).write[TradingPremisesAddress] and
      (__ \ "startOfTrading").write[LocalDate] and
      (__).write[IsResidential]
    ) (unlift(YourTradingPremises.unapply))
}

sealed trait TradingPremisesAddress {

  def toLines: Seq[String] = this match {
    case a: TradingPremisesAddressUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postcode)
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
                                     postcode: String
                             ) extends TradingPremisesAddress

case class TradingPremisesAddressNonUK(
                                  addressLine1: String,
                                  addressLine2: String,
                                  addressLine3: Option[String] = None,
                                  addressLine4: Option[String] = None,
                                  country: String
                                ) extends TradingPremisesAddress

object TradingPremisesAddress{

  val jsonReadsFourAddressLines = {
    (JsPath \ "tradingPremisesAddressLine1").read[String] and
      (JsPath \ "tradingPremisesAddressLine2").read[String] and
      (JsPath \ "tradingPremisesAddressLine3").readNullable[String] and
      (JsPath \ "tradingPremisesAddressLine4").readNullable[String]
  }

  val jsonWritesFourAddressLines = {
    (JsPath \ "tradingPremisesAddressLine1").write[String] and
      (JsPath \ "tradingPremisesAddressLine2").write[String] and
      (JsPath \ "tradingPremisesAddressLine3").writeNullable[String] and
      (JsPath \ "tradingPremisesAddressLine4").writeNullable[String]
  }

  implicit val jsonReadsTradingPremisesAddress : Reads[TradingPremisesAddress]= {
    (JsPath \ "isUK").read[Boolean].flatMap {
      case true => (jsonReadsFourAddressLines and
                    (JsPath \ "postcode").read[String]
                   ).apply(TradingPremisesAddressUK.apply _)

      case false => (jsonReadsFourAddressLines and
                      (JsPath \ "country").read[String]
                    ).apply(TradingPremisesAddressNonUK.apply _)
    }
  }

  implicit val jsonWritesTradingPremisesAddress : Writes[TradingPremisesAddress] = Writes[TradingPremisesAddress] {
      case tpa: TradingPremisesAddressUK => Json.obj(
        "isUK" -> true,
        "tradingPremisesAddressLine1" -> tpa.addressLine1,
        "tradingPremisesAddressLine2" -> tpa.addressLine2,
        "tradingPremisesAddressLine3" -> tpa.addressLine3,
        "tradingPremisesAddressLine4" -> tpa.addressLine4,
        "postcode" -> tpa.postcode)
      case tpa: TradingPremisesAddressNonUK => Json.obj(
        "isUK" -> false,
        "tradingPremisesAddressLine1" -> tpa.addressLine1,
        "tradingPremisesAddressLine2" -> tpa.addressLine2,
        "tradingPremisesAddressLine3" -> tpa.addressLine3,
        "tradingPremisesAddressLine4" -> tpa.addressLine4,
        "country" -> tpa.country)
    }
}

sealed trait IsResidential
case object ResidentialYes extends IsResidential
case object ResidentialNo extends IsResidential

object IsResidential {
  implicit val jsonReadsIsResidential : Reads[IsResidential] = {
    (JsPath \ "isResidential").read[Boolean] fmap {
      case true => ResidentialYes
      case false => ResidentialNo
    }
  }

  implicit val jsonWritesIsResidential : Writes[IsResidential] = Writes[IsResidential] {
    case ResidentialYes => (JsPath \ "isResidential").write[Boolean].writes(true)
    case ResidentialNo => (JsPath \ "isResidential").write[Boolean].writes(false)
  }
}
