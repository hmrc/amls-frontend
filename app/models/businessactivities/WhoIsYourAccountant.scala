package models.businessactivities

case class WhoIsYourAccountant(name: String,
                               tradingName: Option[String],
                               address: AccountantsAddress,
                               alsoDealsWithTax: Boolean) {

}

sealed trait AccountantsAddress

case class UkAccountantsAddress(
                                 addressLine1: String,
                                 addressLine2: String,
                                 addressLine3: Option[String],
                                 addressLine4: Option[String],
                                 postCode: String
                               ) extends AccountantsAddress

case class NonUkAccountantsAddress(
                                 addressLine1: String,
                                 addressLine2: String,
                                 addressLine3: Option[String],
                                 addressLine4: Option[String],
                                 country: String
                               ) extends AccountantsAddress
