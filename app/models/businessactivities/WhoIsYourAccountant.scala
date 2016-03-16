package models.businessactivities

import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.{Reads, JsSuccess, Writes, Json, __}
import play.api.libs.functional.syntax._

sealed trait DoesAccountantAlsoDealWithTax

case object AccountantDoesNotAlsoDealWithTax extends DoesAccountantAlsoDealWithTax

case class AccountantDoesAlsoDealWithTax(accountantsRef: String) extends DoesAccountantAlsoDealWithTax

object DoesAccountantAlsoDealWithTax {
  import play.api.data.mapping.forms.Rules._

  val accountantRefNoType = notEmpty compose maxLength(11) compose minLength(11)

  implicit val formRule: Rule[UrlFormEncoded, DoesAccountantAlsoDealWithTax] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "alsoDealsWithTax").read[Boolean] flatMap {
      case false => Rule.fromMapping {_ => Success(AccountantDoesNotAlsoDealWithTax)}
      case true => ((__ \ "accountantsReferenceNumber").read(accountantRefNoType)) fmap AccountantDoesAlsoDealWithTax.apply
    }
  }

  implicit val formWrites: Write[DoesAccountantAlsoDealWithTax, UrlFormEncoded] = Write {
    case AccountantDoesAlsoDealWithTax(refNo) => Map("alsoDealsWithTax" -> Seq("true"), "accountantsReferenceNumber" -> Seq(refNo))
    case AccountantDoesNotAlsoDealWithTax => Map("alsoDealsWithTax" -> Seq("false"))
  }

  implicit val jsonReads: Reads[DoesAccountantAlsoDealWithTax] =
    (__ \ "doesAccountantAlsoDealWithTax").read[Boolean] flatMap {
      case true => (__ \ "accountantsReference").read[String] map AccountantDoesAlsoDealWithTax.apply
      case false => Reads(_ => JsSuccess(AccountantDoesNotAlsoDealWithTax))
    }

  implicit val jsonWrites = Writes[DoesAccountantAlsoDealWithTax] {
    case AccountantDoesAlsoDealWithTax(ref) => Json.obj(
      "doesAccountantAlsoDealWithTax" -> true,
      "accountantsReference" -> ref
    )
    case AccountantDoesNotAlsoDealWithTax => Json.obj("doesAccountantAlsoDealWithTax" -> false)
  }
}

case class WhoIsYourAccountant(accountantsName: String,
                               accountantsTradingName: Option[String],
                               address: AccountantsAddress,
                               alsoDealsWithTax: DoesAccountantAlsoDealWithTax) {


  def name(newName: String) : WhoIsYourAccountant = this.copy(accountantsName = newName)
  def tradingName(newTradingName: String) : WhoIsYourAccountant = this.copy(accountantsTradingName = Some(newTradingName))
  def address(newAddress: AccountantsAddress) : WhoIsYourAccountant = this.copy(address = newAddress)
  def alsoDealsWithTax(newAlsoDealsWithTax: DoesAccountantAlsoDealWithTax) : WhoIsYourAccountant = this.copy(alsoDealsWithTax = newAlsoDealsWithTax)
}

object WhoIsYourAccountant {

  import play.api.libs.json._

  val key = "who-is-your-accountant"

  implicit val jsonWrites : Writes[WhoIsYourAccountant] = Writes[WhoIsYourAccountant] { data:WhoIsYourAccountant =>
    Json.obj("accountantsName" -> data.accountantsName,
             "accountantsTradingName" -> data.accountantsTradingName) ++
      Json.toJson(data.address).as[JsObject] ++
      Json.toJson(data.alsoDealsWithTax).as[JsObject]
  }

  implicit val jsonReads : Reads[WhoIsYourAccountant] = (
    ((__ \ "accountantsName").read[String] and
     (__ \ "accountantsTradingName").read[Option[String]] and
     (__).read[AccountantsAddress] and
     (__).read[DoesAccountantAlsoDealWithTax])(WhoIsYourAccountant.apply _)
    )

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
          "country" -> Seq(address.country)
        )
      }) ++ DoesAccountantAlsoDealWithTax.formWrites.writes(data.alsoDealsWithTax)
    }

  implicit val formRule: Rule[UrlFormEncoded, WhoIsYourAccountant] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._

      val nameType = notEmpty compose maxLength(140)

      ((__ \ "name").read(nameType) and
        (__ \ "tradingName").read(optionR(nameType)) and
        (__ ).read[AccountantsAddress] and
        (__).read[DoesAccountantAlsoDealWithTax])(WhoIsYourAccountant.apply _)
    }
}


