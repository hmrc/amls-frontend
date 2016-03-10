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
    (__ \ "isUK").read[Boolean] flatMap {
      case true => (
        (__ \ "addressLine1").read[String] and
          (__ \ "addressLine2").read[String] and
          (__ \ "addressLine3").readNullable[String] and
          (__ \ "addressLine4").readNullable[String] and
          (__ \ "postCode").read[String]
        ) (UkAccountantsAddress.apply _)
      case false => (
        (__ \ "addressLine1").read[String] and
          (__ \ "addressLine2").read[String] and
          (__ \ "addressLine3").readNullable[String] and
          (__ \ "addressLine4").readNullable[String] and
          (__ \ "country").read[String]
        ) (NonUkAccountantsAddress.apply _)
    }
  }

  implicit val jsonWrites: Writes[AccountantsAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[AccountantsAddress] {
      case a: UkAccountantsAddress =>
        (
            (__ \ "addressLine1").write[String] and
            (__ \ "addressLine2").write[String] and
            (__ \ "addressLine3").writeNullable[String] and
            (__ \ "addressLine4").writeNullable[String] and
            (__ \ "postCode").write[String]
          ) (unlift(UkAccountantsAddress.unapply)).writes(a)
      case a: NonUkAccountantsAddress =>
        (
            (__ \ "addressLine1").write[String] and
            (__ \ "addressLine2").write[String] and
            (__ \ "addressLine3").writeNullable[String] and
            (__ \ "addressLine4").writeNullable[String] and
            (__ \ "country").write[String]
          ) (unlift(NonUkAccountantsAddress.unapply)).writes(a)
    }
  }
}