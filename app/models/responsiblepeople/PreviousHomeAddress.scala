package models.responsiblepeople

import models.FormTypes._
import play.api.data.mapping._
import play.api.data.mapping.forms._
import play.api.data.validation.ValidationError
import play.api.libs.json.{Writes, Reads}
import utils.TraversableValidators._

sealed trait TimeAtAddress {
  val value: String =
    this match {
      case ZeroToSixMonths => "0-6 months"
      case SevenToTwelveMonths => "7-12 months"
      case OneToThreeYears => "1-3 years"
      case ThreeYearsPlus => "3+ years"
    }
}

case object ZeroToSixMonths extends TimeAtAddress
case object SevenToTwelveMonths extends TimeAtAddress
case object OneToThreeYears extends TimeAtAddress
case object ThreeYearsPlus extends TimeAtAddress

sealed trait PreviousHomeAddress

case class PreviousHomeAddressUK(
                                 addressLine1: String,
                                 addressLine2: String,
                                 addressLine3: Option[String],
                                 addressLine4: Option[String],
                                 postCode: String,
                                 timeAtAddress: TimeAtAddress
                               ) extends PreviousHomeAddress

case class PreviousHomeAddressNonUK(
                                    addressLine1: String,
                                    addressLine2: String,
                                    addressLine3: Option[String],
                                    addressLine4: Option[String],
                                    country: String,
                                    timeAtAddress: TimeAtAddress
                                   ) extends PreviousHomeAddress


object PreviousHomeAddress {

  implicit val formRule: Rule[UrlFormEncoded, PreviousHomeAddress] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isUK").read[Boolean] flatMap {
      case true =>
        (
          (__ \ "addressLine1").read(addressType) and
            (__ \ "addressLine2").read(addressType) and
            (__ \ "addressLine3").read(optionR(addressType)) and
            (__ \ "addressLine4").read(optionR(addressType)) and
            (__ \ "postCode").read(postcodeType) and
            (__ \ "timeAtAddress").read(minLength[Set[String]](1)) flatMap { time =>
            time.map {
              case "01" => Rule[UrlFormEncoded, TimeAtAddress](_ => Success(ZeroToSixMonths))
              case "02" => Rule[UrlFormEncoded, TimeAtAddress](_ => Success(SevenToTwelveMonths))
              case "03" => Rule[UrlFormEncoded, TimeAtAddress](_ => Success(OneToThreeYears))
              case "04" => Rule[UrlFormEncoded, TimeAtAddress](_ => Success(ThreeYearsPlus))
              case _ =>
                Rule[UrlFormEncoded, TimeAtAddress] { _ =>
                  Failure(Seq((Path \ "timeAtAddress") -> Seq(ValidationError("error.invalid"))))
                }
            }.foldLeft[Rule[UrlFormEncoded, Set[TimeAtAddress]]](
              Rule[UrlFormEncoded, Set[TimeAtAddress]](_ => Success(Set.empty))
            ) {
              case (m, n) =>
                n flatMap { x =>
                  m fmap {
                    _ + x
                  }
                }
            }
          }
            )

          ) (PreviousHomeAddressUK.apply _)
      case false =>
        (
          (__ \ "addressLineNonUK1").read(addressType) and
            (__ \ "addressLineNonUK2").read(addressType) and
            (__ \ "addressLineNonUK3").read(optionR(addressType)) and
            (__ \ "addressLineNonUK4").read(optionR(addressType)) and
            (__ \ "country").read(countryType) and
            (__ \ "timeAtAddress").read(timeAtAddressType)
          ) (PreviousHomeAddressNonUK.apply _)
    }
  }

  implicit val formWrites: Write[PreviousHomeAddress, UrlFormEncoded] = Write {
    case uk: PreviousHomeAddressUK => Map("isUK" -> Seq("true"))
    case non: PreviousHomeAddressNonUK => Map("isUK" -> Seq("false"))
  }


  implicit val jsonReads: Reads[PreviousHomeAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (__ \ "previousAddressPostCode").read[String] andKeep (
      ((__ \ "previousAddressLine1").read[String] and
        (__ \ "previousAddressLine2").read[String] and
        (__ \ "previousAddressLine3").readNullable[String] and
        (__ \ "previousAddressLine4").readNullable[String] and
        (__ \ "previousAddressPostCode").read[String] and
        (__ \ "previousTimeAtAddress").read[String]) (PreviousHomeAddressUK.apply _) map identity[PreviousHomeAddress]
      ) orElse
      ((__ \ "previousAddressLine1").read[String] and
        (__ \ "previousAddressLine2").read[String] and
        (__ \ "previousAddressLine3").readNullable[String] and
        (__ \ "previousAddressLine4").readNullable[String] and
        (__ \ "previousAddressCountry").read[String] and
        (__ \ "previousTimeAtAddress").read[String]) (PreviousHomeAddressNonUK.apply _)

  }

  implicit val jsonWrites: Writes[PreviousHomeAddress] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[PreviousHomeAddress] {
      case a: PreviousHomeAddressUK =>
        (
          (__ \ "previousAddressLine1").write[String] and
            (__ \ "previousAddressLine2").write[String] and
            (__ \ "previousAddressLine3").writeNullable[String] and
            (__ \ "previousAddressLine4").writeNullable[String] and
            (__ \ "previousAddressPostCode").write[String] and
            (__ \ "previousTimeAtAddress").write[String]
          ) (unlift(PreviousHomeAddressUK.unapply)).writes(a)
      case a: PreviousHomeAddressNonUK =>
        (
          (__ \ "previousAddressLine1").write[String] and
            (__ \ "previousAddressLine2").write[String] and
            (__ \ "previousAddressLine3").writeNullable[String] and
            (__ \ "previousAddressLine4").writeNullable[String] and
            (__ \ "previousAddressCountry").write[String] and
            (__ \ "previousTimeAtAddress").write[String]
          ) (unlift(PreviousHomeAddressNonUK.unapply)).writes(a)
    }
  }
}