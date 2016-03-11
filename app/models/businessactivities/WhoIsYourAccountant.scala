package models.businessactivities

import play.api.libs.json._

sealed trait DoesAccountantAlsoDealWithTax

case object AccountantDoesNotAlsoDealWithTax extends DoesAccountantAlsoDealWithTax

case class AccountantDoesAlsoDealWithTax(accountantsRef: String) extends DoesAccountantAlsoDealWithTax

object DoesAccountantAlsoDealWithTax {

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

case class WhoIsYourAccountant(name: String,
                               tradingName: Option[String],
                               address: AccountantsAddress,
                               alsoDealsWithTax: DoesAccountantAlsoDealWithTax) {


  def name(newName: String) : WhoIsYourAccountant = this.copy(name = newName)
  def tradingName(newTradingName: String) : WhoIsYourAccountant = this.copy(tradingName = Some(newTradingName))
  def address(newAddress: AccountantsAddress) : WhoIsYourAccountant = this.copy(address = newAddress)
  def alsoDealsWithTax(newAlsoDealsWithTax: DoesAccountantAlsoDealWithTax) : WhoIsYourAccountant = this.copy(alsoDealsWithTax = newAlsoDealsWithTax)
}

object WhoIsYourAccountant {

  import play.api.libs.json._

  val key = "who-is-your-accountant"

  implicit val formats = Json.format[WhoIsYourAccountant]
}


