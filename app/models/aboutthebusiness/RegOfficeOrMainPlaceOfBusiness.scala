package models.aboutthebusiness

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class BCAddress(
                      line_1: String,
                      line_2: String,
                      line_3: Option[String] = None,
                      line_4: Option[String] = None,
                      postcode: Option[String] = None,
                      country: String) {
}

object BCAddress {
  implicit val formats = Json.format[BCAddress]
}

case class BusinessCustomerDetails(businessName: String,
                                   businessType: Option[String],
                                   businessAddress: BCAddress,
                                   sapNumber: String,
                                   safeId: String,
                                   agentReferenceNumber: Option[String],
                                   firstName: Option[String] = None,
                                   lastName: Option[String] = None)

object BusinessCustomerDetails {
  implicit val formats = Json.format[BusinessCustomerDetails]
}

case class RegOfficeOrMainPlaceOfBusiness (isRegOfficeOrMainPlaceOfBusiness: Boolean )

object RegOfficeOrMainPlaceOfBusiness {

  implicit val formats = Json.format[RegOfficeOrMainPlaceOfBusiness]

  implicit val formRule: Rule[UrlFormEncoded, RegOfficeOrMainPlaceOfBusiness] = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.forms.Rules._
    (__ \ "isRegOfficeOrMainPlaceOfBusiness").read[Boolean].fmap { b => RegOfficeOrMainPlaceOfBusiness(b) }
  }

  implicit val formWrites: Write[RegOfficeOrMainPlaceOfBusiness, UrlFormEncoded] = Write {
    case RegOfficeOrMainPlaceOfBusiness(b) => Map("isRegOfficeOrMainPlaceOfBusiness" -> Seq(b.toString))
  }
}