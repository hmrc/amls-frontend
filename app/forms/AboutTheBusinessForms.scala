package forms

import config.AmlsPropertiesReader._
import models._
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.BooleanWithTextValidator._
import utils.validation.BooleanValidator._
import utils.validation.PhoneNumberValidator._
import utils.validation.{EmailValidator, NumberValidator, WebAddressValidator}

object AboutTheBusinessForms {
  val businessHasWebsiteForm = Form(mapping(
    "hasWebsite" -> mandatoryBooleanWithText("website", "true",
      "err.required", "err.required", "err.notrequired"),
    "website" -> optional(WebAddressValidator.webAddress("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthWebAddress")))
  )(BusinessHasWebsite.apply)(BusinessHasWebsite.unapply))

  val telephoningBusinessForm = Form(mapping(
    "businessPhoneNumber" -> mandatoryPhoneNumber("err.required", "err.invalidLength",
      "aboutthebusiness.telephoning.invalid.phone", getIntFromProperty("validationMaxLengthPhoneNo")),
    "mobileNumber" -> optional(mandatoryPhoneNumber("err.required", "err.invalidLength",
      "aboutthebusiness.telephoning.invalid.phone", getIntFromProperty("validationMaxLengthPhoneNo")))
  )(TelephoningBusiness.apply)(TelephoningBusiness.unapply))

  val confirmingYourAddressForm = Form(mapping(
    "isRegOfficeOrMainPlaceOfBusiness" -> mandatoryBoolean("err.required")
  )(ConfirmingYourAddress.apply)(ConfirmingYourAddress.unapply))

  val businessRegForVATForm = Form(mapping(
    "hasVAT" -> mandatoryBooleanWithText("VATNum", "true", "err.required", "err.required", "err.notrequired"),
    "VATNum" -> optional(NumberValidator.validateNumber("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthVAT"), getIntFromProperty("validationMaxLengthVAT")))
  )(BusinessWithVAT.apply)(BusinessWithVAT.unapply))

  val businessHasEmailForm = Form(mapping(
    "email" -> EmailValidator.mandatoryEmail("err.required", "err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthEmail"))
  )(BusinessHasEmail.apply)(BusinessHasEmail.unapply))

  val registeredWithHMRCBeforeForm = Form(mapping(
    "registeredWithHMRC" -> mandatoryBooleanWithText("mlrNumber", "true", "err.required", "err.required", "err.notrequired"),
    "mlrNumber" -> optional(NumberValidator.validateNumber("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMinLengthMLR"), getIntFromProperty("validationMaxLengthMLR")))
  )(RegisteredWithHMRCBefore.apply)(RegisteredWithHMRCBefore.unapply))
}
