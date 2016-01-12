package models
import play.api.libs.json.Json

import scala.language.implicitConversions

case class BCAddress(
                      line_1: String,
                      line_2: String,
                      line_3: Option[String] = None,
                      line_4: Option[String] = None,
                      postcode: Option[String] = None,
                      country: String) {

  def render = {
    val line3display = line_3.map(line3 => s"$line3, ").getOrElse("")
    val line4display = line_4.map(line4 => s"$line4, ").getOrElse("")
    val postcodeDisplay = postcode.map(postcode1 => s"$postcode1, ").getOrElse("")
    s"$line_1, $line_2, $line3display$line4display$postcodeDisplay$country"
  }
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

case class BusinessHasWebsite(hasWebsite: Boolean, website: Option[String])

object BusinessHasWebsite {
  implicit val formats = Json.format[BusinessHasWebsite]
}

case class TelephoningBusiness(businessPhoneNumber: String,
                               mobileNumber: Option[String])

object TelephoningBusiness {
  implicit val formats = Json.format[TelephoningBusiness]
}

/**
 * This created because the address is not entered at front end so isn't in form.
 */
case class ConfirmingYourAddressSave4Later(registeredOfficeAddress: BCAddress,
                                           isRegOfficeOrMainPlaceOfBusiness: Boolean)

object ConfirmingYourAddressSave4Later {
  implicit val formats = Json.format[ConfirmingYourAddressSave4Later]
}


case class ConfirmingYourAddress(isRegOfficeOrMainPlaceOfBusiness: Boolean)

object ConfirmingYourAddress {

  implicit val formats = Json.format[ConfirmingYourAddress]

  def fromConfirmingYourAddressSave4Later(confirmingYourAddressSave4Later: ConfirmingYourAddressSave4Later): ConfirmingYourAddress = {
    ConfirmingYourAddress(confirmingYourAddressSave4Later.isRegOfficeOrMainPlaceOfBusiness)
  }

}

case class BusinessWithVAT(hasVAT: Boolean, VATNum: Option[String])

object BusinessWithVAT {
  implicit val formats = Json.format[BusinessWithVAT]
}

case class BusinessHasEmail(email: String)

object BusinessHasEmail {
  implicit val formats = Json.format[BusinessHasEmail]
}

case class RegisteredWithHMRCBefore(registeredWithHMRC: Boolean, mlrNumber: Option[String])

object RegisteredWithHMRCBefore {
  implicit val formats = Json.format[RegisteredWithHMRCBefore]
}

