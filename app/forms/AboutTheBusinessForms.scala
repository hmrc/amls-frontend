package forms

import models._
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.BooleanWithTextValidator._
import utils.validation.PhoneNumberValidator._
import utils.validation.WebAddressValidator

object AboutTheBusinessForms {

  val businessHasWebsiteFormMapping = mapping(
    "hasWebsite" -> mandatoryBooleanWithText("website", "true",
      "error.required", "error.required", "error.notrequired"),
    "website" -> optional(WebAddressValidator.webAddress("err.invalidLength", "error.invalid"))
  )(BusinessHasWebsite.apply)(BusinessHasWebsite.unapply)

  val businessHasWebsiteForm = Form(businessHasWebsiteFormMapping)

  val telephoningBusinessForm = Form(mapping(
    "businessPhoneNumber" -> mandatoryPhoneNumber("error.required", "err.invalidLength", "telephoningbusiness.error.invalidphonenumber"),
    "mobileNumber" -> optional(mandatoryPhoneNumber("error.required", "err.invalidLength", "telephoningbusiness.error.invalidphonenumber"))
  )(TelephoningBusiness.apply)(TelephoningBusiness.unapply))

  val registeredOfficeForm = Form(mapping(
    "registeredOfficeAddress" -> mapping(
      "line_1" -> text,
      "line_2" -> text,
      "line_3" -> optional(text),
      "line_4" -> optional(text),
      "postcode" -> optional(text),
      "country" -> text)(BCAddress.apply)(BCAddress.unapply),
    "isRegisteredOffice" -> text
  )(RegisteredOffice.applyString)(RegisteredOffice.unapplyString))

}
