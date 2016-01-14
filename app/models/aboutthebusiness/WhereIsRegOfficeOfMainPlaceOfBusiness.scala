package models.aboutthebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, To, Write}
import play.api.libs.json.Json


case class WhereIsRegOfficeOfMainPlaceOfBusiness {
                                              isUKOrOverseas : Boolean,
                                              addressLine1: String,
                                              addressLine2: String,
                                              addressLine3: String,
                                              addressLine4: String,
                                              postCode: Option[String],
                                              country: Option[String]
                                            }

object WhereIsRegOfficeOfMainPlaceOfBusiness {
  implicit val formats = Json.format[WhereIsRegOfficeOfMainPlaceOfBusiness]

  implicit val formRule: Rule[UrlFormEncoded, WhereIsRegOfficeOfMainPlaceOfBusiness] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    import models.FormTypes._
    (
      (__ \ "isUKOrOverseas").read(indivNameType) and
        (__ \ "addressLine1").read[Option[String]] and
        (__ \ "addressLine2").read(indivNameType) and
        (__ \ "addressLine3").read(indivNameType) and
        (__ \ "addressLine4").read(indivNameType) and
        (__ \ "postCode").read(indivNameType) and
        (__ \ "country").read(indivNameType)

      )(WhereIsRegOfficeOfMainPlaceOfBusiness.apply _)
  }

  implicit val formWrites: Write[WhereIsRegOfficeOfMainPlaceOfBusiness, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "firstName").write[String] and
        (__ \ "middleName").write[Option[String]] and
        (__ \ "lastName").write[String]
      )(unlift(WhereIsRegOfficeOfMainPlaceOfBusiness.unapply _))
  }
}