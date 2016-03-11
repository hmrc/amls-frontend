package models.businessactivities

case class WhoIsYourAccountant(name: String,
                               tradingName: Option[String],
                               address: AccountantsAddress,
                               alsoDealsWithTax: Boolean) {


  def name(newName: String) : WhoIsYourAccountant = this.copy(name = newName)
  def tradingName(newTradingName: String) : WhoIsYourAccountant = this.copy(tradingName = Some(newTradingName))
  def address(newAddress: AccountantsAddress) : WhoIsYourAccountant = this.copy(address = newAddress)
  def alsoDealsWithTax(newAlsoDealsWithTax: Boolean) : WhoIsYourAccountant = this.copy(alsoDealsWithTax = newAlsoDealsWithTax)

}

object WhoIsYourAccountant {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "who-is-your-accountant"

  implicit val formats = Json.format[WhoIsYourAccountant]
}