package models.aboutyou


import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.Json

case class YourDetails(
                        firstName: String,
                        middleName: Option[String],
                        lastName: String
                      )

object YourDetails {

  implicit val formats = Json.format[YourDetails]

  implicit val formRule: Rule[UrlFormEncoded, YourDetails] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (
      (__ \ "firstName").read(indivNameType) and
        (__ \ "middleName").read[Option[String]] and
        (__ \ "lastName").read(indivNameType)
      )(YourDetails.apply _)
  }

  implicit val formWrites: Write[YourDetails, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "firstName").write[String] and
        (__ \ "middleName").write[Option[String]] and
        (__ \ "lastName").write[String]
      )(unlift(YourDetails.unapply _))
  }
}