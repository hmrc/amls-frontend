package forms

import config.AmlsPropertiesReader._
import models._
import models.aboutthebusiness.RegOfficeOrMainPlaceOfBusiness
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.BooleanWithTextValidator._
import utils.validation.BooleanValidator._
import utils.validation.PhoneNumberValidator._
import utils.validation.{EmailValidator, NumberValidator, WebAddressValidator}

object AboutTheBusinessForms {

  val confirmingYourAddressForm = Form(mapping(
    "isRegOfficeOrMainPlaceOfBusiness" -> mandatoryBoolean("err.required")
  )(RegOfficeOrMainPlaceOfBusiness.apply)(RegOfficeOrMainPlaceOfBusiness.unapply))

  val businessRegForVATForm = Form(mapping(
    "hasVAT" -> mandatoryBooleanWithText("VATNum", "true", "err.required", "err.required", "err.notrequired"),
    "VATNum" -> optional(NumberValidator.validateNumber("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthVAT"), getIntFromProperty("validationMaxLengthVAT")))
  )(BusinessWithVAT.apply)(BusinessWithVAT.unapply))

  val contactingYouForm = Form(mapping(
    "phoneNumber" -> mandatoryPhoneNumber("err.required", "err.invalidLength",
      "aboutthebusiness.telephoning.invalid.phone", getIntFromProperty("validationMaxLengthPhoneNo")),
    "email" -> EmailValidator.mandatoryEmail("err.required", "err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthEmail")),
    "website" -> optional(WebAddressValidator.webAddress("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthWebAddress"))),
    "letterToThisAddress" -> mandatoryBoolean("err.required")
  )(ContactingYou.apply)(ContactingYou.unapply))

  val registeredWithHMRCBeforeForm = Form(mapping(
    "registeredWithHMRC" -> mandatoryBooleanWithText("mlrNumber", "true", "err.required", "err.required", "err.notrequired"),
    "mlrNumber" -> optional(NumberValidator.validateNumber("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMinLengthMLR"), getIntFromProperty("validationMaxLengthMLR")))
  )(RegisteredWithHMRCBefore.apply)(RegisteredWithHMRCBefore.unapply))
}
