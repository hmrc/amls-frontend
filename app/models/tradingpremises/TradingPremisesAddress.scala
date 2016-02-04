package models.tradingpremises

import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.{Reads, Writes}

sealed trait TradingPremisesAddress {

  def toLines: Seq[String] = this match {
    case a: UKTradingPremises =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        a.postcode,
        Some(a.country)
      ).flatten

    case a: NonUKTradingPremises =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        a.postcode,
        Some(a.country)
      ).flatten

  }
}

case class UKTradingPremises(
                              addressLine1: String,
                              addressLine2: String,
                              addressLine3: Option[String] = None,
                              addressLine4: Option[String] = None,
                              postcode: Option[String],
                              country: String
                            ) extends TradingPremisesAddress

case class NonUKTradingPremises(
                                 addressLine1: String,
                                 addressLine2: String,
                                 addressLine3: Option[String] = None,
                                 addressLine4: Option[String] = None,
                                 postcode: Option[String],
                                 country: String
                               ) extends TradingPremisesAddress


object TradingPremisesAddress {

  implicit val jsonReadsTradingPremisesAddress: Reads[TradingPremisesAddress] = {
    println("TPA HERE ")
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (
    (JsPath \ "addressLine1").read[String] and
      (JsPath \ "addressLine2").read[String] and
      (JsPath \ "addressLine3").readNullable[String] and
      (JsPath \ "addressLine4").readNullable[String] and
      (JsPath \ "postcode").read[Option[String]] and
      (JsPath \ "country").read[String]
    ) (UKTradingPremises.apply _)

  }

  implicit val jsonWritesTradingPremisesAddress: Writes[TradingPremisesAddress] = {

    import play.api.libs.json.Writes._
    import play.api.libs.json._


    Writes[TradingPremisesAddress] {
      case tpa: UKTradingPremises => Json.obj(
        "addressLine1" -> tpa.addressLine1,
        "addressLine2" -> tpa.addressLine2,
        "addressLine3" -> tpa.addressLine3,
        "addressLine4" -> tpa.addressLine4,
        "postcode" -> tpa.postcode,
        "country" -> tpa.country)
      case tpa: NonUKTradingPremises => Json.obj(
        "addressLine1" -> tpa.addressLine1,
        "addressLine2" -> tpa.addressLine2,
        "addressLine3" -> tpa.addressLine3,
        "addressLine4" -> tpa.addressLine4,
        "postcode" -> tpa.postcode,
        "country" -> tpa.country)
    }
  }

  implicit val formRule: Rule[UrlFormEncoded, TradingPremisesAddress] =
    From[UrlFormEncoded] { __ =>
      import models.FormTypes._
      import play.api.data.mapping.forms.Rules._

      val nameType = notEmpty compose maxLength(140)

      (__ \ "isUK").read[Boolean] flatMap {
        case true => (
          (__ \ "addressLine1").read(addressType) ~
            (__ \ "addressLine2").read(addressType) ~
            (__ \ "addressLine3").read(optionR(addressType)) ~
            (__ \ "addressLine4").read(optionR(addressType)) ~
            (__ \ "postCode").read(optionR(postCodeType)) ~
            (__ \ "country").read(countryType)
          ) (UKTradingPremises.apply _)
        case false => (
          (__ \ "addressLine1").read(addressType) ~
            (__ \ "addressLine2").read(addressType) ~
            (__ \ "addressLine3").read(optionR(addressType)) ~
            (__ \ "addressLine4").read(optionR(addressType)) ~
            (__ \ "postCode").read(optionR(postCodeType)) ~
            (__ \ "country").read(countryType)
          ) (NonUKTradingPremises.apply _)
      }
    }


  implicit val formWrites = Write[TradingPremisesAddress, UrlFormEncoded] {
    case tpa: UKTradingPremises =>
      Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq(tpa.addressLine1),
        "addressLine2" -> Seq(tpa.addressLine2),
        "addressLine3" -> tpa.addressLine3.toSeq,
        "addressLine4" -> tpa.addressLine4.toSeq,
        "postcode" -> tpa.postcode.toSeq,
        "country" -> Seq(tpa.country)
      )
    case tpa: NonUKTradingPremises =>
      Map(
        "isUK" -> Seq("true"),
        "addressLine1" -> Seq(tpa.addressLine1),
        "addressLine2" -> Seq(tpa.addressLine2),
        "addressLine3" -> tpa.addressLine3.toSeq,
        "addressLine4" -> tpa.addressLine4.toSeq,
        "postcode" -> tpa.postcode.toSeq,
        "country" -> Seq(tpa.country)
      )
  }
}