package models.businessactivities

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import models.FormTypes._
import play.api.libs.functional.syntax._

case class WhoIsYourAccountant(accountantsName: String,
                               accountantsTradingName: Option[String],
                               address: AccountantsAddress) {

  def name(newName: String) : WhoIsYourAccountant = this.copy(accountantsName = newName)
  def tradingName(newTradingName: String) : WhoIsYourAccountant = this.copy(accountantsTradingName = Some(newTradingName))
  def address(newAddress: AccountantsAddress) : WhoIsYourAccountant = this.copy(address = newAddress)
}

object WhoIsYourAccountant {

  import play.api.libs.json._

  val key = "who-is-your-accountant"

  implicit val jsonWrites : Writes[WhoIsYourAccountant] = Writes[WhoIsYourAccountant] { data:WhoIsYourAccountant =>
    Json.obj("accountantsName" -> data.accountantsName,
             "accountantsTradingName" -> data.accountantsTradingName
    ) ++ Json.toJson(data.address).as[JsObject]
  }

  implicit val jsonReads : Reads[WhoIsYourAccountant] =
    ((__ \ "accountantsName").read[String] and
     (__ \ "accountantsTradingName").readNullable[String] and
     __.read[AccountantsAddress])(WhoIsYourAccountant.apply _)


  implicit val formWrites = Write[WhoIsYourAccountant, UrlFormEncoded] {
    data: WhoIsYourAccountant =>

      Map(
        "name" -> Seq(data.accountantsName),
        "tradingName" -> data.accountantsTradingName.toSeq
      ) ++ (data.address match {
        case address: UkAccountantsAddress => Map(
          "isUK" -> Seq("true"),
          "addressLine1" -> Seq(address.addressLine1),
          "addressLine2" -> Seq(address.addressLine2),
          "addressLine3" -> address.addressLine3.toSeq,
          "addressLine4" -> address.addressLine4.toSeq,
          "postCode" -> Seq(address.postCode)
        )
        case address: NonUkAccountantsAddress => Map(
          "isUK" -> Seq("false"),
          "addressLine1" -> Seq(address.addressLine1),
          "addressLine2" -> Seq(address.addressLine2),
          "addressLine3" -> address.addressLine3.toSeq,
          "addressLine4" -> address.addressLine4.toSeq,
          "country" -> Seq(address.country.code)
        )
      })
    }

  implicit val formRule: Rule[UrlFormEncoded, WhoIsYourAccountant] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      import utils.MappingUtils.Implicits._

      val nameTypeLength = 140
      val tradingNameTypeLength = 120

      val nameType = notEmptyStrip andThen
      notEmpty.withMessage("error.required.ba.advisor.name") andThen
      maxLength(nameTypeLength).withMessage("error.invalid.maxlength.140") andThen
      basicPunctuationPattern()

      val tradingNameType = notEmptyStrip andThen
        maxLength(tradingNameTypeLength).withMessage("error.invalid.maxlength.120") andThen
        basicPunctuationPattern()

      ((__ \ "name").read(nameType) ~
        (__ \ "tradingName").read(optionR(tradingNameType)) ~
        __.read[AccountantsAddress])(WhoIsYourAccountant.apply _)
    }
}



