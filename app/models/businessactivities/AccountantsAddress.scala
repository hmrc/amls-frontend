package models.businessactivities

import play.api.libs.json.{Writes, Reads}

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


object AccountantsAddress extends AccountantsAddress {

  implicit val jsonReads: Reads[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "accountantsAddressPostCode").read[String] andKeep (
        ((__ \ "accountantsAddressLine1").read[String] and
        (__ \ "accountantsAddressLine2").read[String] and
        (__ \ "accountantsAddressLine3").readNullable[String] and
        (__ \ "accountantsAddressLine4").readNullable[String] and
        (__ \ "accountantsAddressPostCode").read[String])  (UkAccountantsAddress.apply _) map identity[AccountantsAddress]
      ) orElse
        ( (__ \ "accountantsAddressLine1").read[String] and
          (__ \ "accountantsAddressLine2").read[String] and
          (__ \ "accountantsAddressLine3").readNullable[String] and
          (__ \ "accountantsAddressLine4").readNullable[String] and
          (__ \ "accountantsAddressCountry").read[String]) (NonUkAccountantsAddress.apply _)

  }

  implicit val jsonWrites: Writes[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[AccountantsAddress] {
      case a: UkAccountantsAddress =>
        (
            (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").write[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressPostCode").write[String]
          ) (unlift(UkAccountantsAddress.unapply)).writes(a)
      case a: NonUkAccountantsAddress =>
        (
            (__ \ "accountantsAddressLine1").write[String] and
            (__ \ "accountantsAddressLine2").write[String] and
            (__ \ "accountantsAddressLine3").writeNullable[String] and
            (__ \ "accountantsAddressLine4").writeNullable[String] and
            (__ \ "accountantsAddressCountry").write[String]
          ) (unlift(NonUkAccountantsAddress.unapply)).writes(a)
    }
  }
}