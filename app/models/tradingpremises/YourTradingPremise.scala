package models.tradingpremises

import org.joda.time.LocalDate
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{Success, Path, Rule}
import play.api.libs.functional.FunctionalBuilder
import play.api.libs.json._
import play.api.libs.functional.syntax._
import models.FormTypes._
import play.api.data.mapping.forms.Rules._

case class YourTradingPremises(tradingName : String,
                              tradingAddress: TradingPremisesAddress,
                              startOfTradingDate: LocalDate,
                              isResidential : IsResidential)

object YourTradingPremises{
  implicit val jsonReadsYourTradingPremises = {
    ((JsPath \ "tradingName").read[String] and
      JsPath.read[TradingPremisesAddress] and
      (JsPath \ "startOfTrading").read[LocalDate] and
      (JsPath).read[IsResidential])(YourTradingPremises.apply _)
  }

  //TODO - Joe: Implement Writes
  implicit val jsonWritesYourTradingPremises : Writes[YourTradingPremises] = OWrites({
    x:YourTradingPremises => {x match {case _ => Json.obj("fdsfsd" -> "fdsfsf")}}
  })
}

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

object TradingPremisesAddress{

  val jsonReadsFourAddressLines = {
    (JsPath \ "tradingPremisesAddressLine1").read[String] and
      (JsPath \ "tradingPremisesAddressLine2").read[String] and
      (JsPath \ "tradingPremisesAddressLine3").readNullable[String] and
      (JsPath \ "tradingPremisesAddressLine4").readNullable[String]
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
}
