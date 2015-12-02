package forms

import config.AmlsPropertiesReader._
import models.{BusinessHasEmail, BusinessHasWebsite, BusinessWithVAT, TelephoningBusiness}
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.BooleanWithTextValidator._
import utils.validation.PhoneNumberValidator._
import utils.validation.{EmailValidator, VATNumberValidator, WebAddressValidator}

object AboutTheBusinessForms {

  val businessHasWebsiteFormMapping = mapping(
    "hasWebsite" -> mandatoryBooleanWithText("website", "true",
      "error.required", "error.required", "error.notrequired"),
    "website" -> optional(WebAddressValidator.webAddress("err.invalidLength", "error.invalid", getProperty("validationMaxLengthWebAddress").toInt))
  )(BusinessHasWebsite.apply)(BusinessHasWebsite.unapply)

  val businessHasWebsiteForm = Form(businessHasWebsiteFormMapping)

  val telephoningBusinessForm = Form(mapping(
    "businessPhoneNumber" -> mandatoryPhoneNumber("error.required", "err.invalidLength",
      "telephoningbusiness.error.invalidphonenumber", getProperty("validationMaxLengthPhoneNo").toInt),
    "mobileNumber" -> optional(mandatoryPhoneNumber("error.required", "err.invalidLength",
      "telephoningbusiness.error.invalidphonenumber", getProperty("validationMaxLengthPhoneNo").toInt))
  )(TelephoningBusiness.apply)(TelephoningBusiness.unapply))

  val businessRegForVATFormMapping = mapping(
    "hasVAT" -> mandatoryBooleanWithText("VATNum", "true",
      "error.required", "error.required", "error.notrequired"),
    "VATNum" -> optional(VATNumberValidator.vatNumber("err.invalidLength", "error.invalid", getProperty("validationMaxLengthVAT").toInt))
  )(BusinessWithVAT.apply)(BusinessWithVAT.unapply)

  val businessRegForVATForm = Form(businessRegForVATFormMapping)

  val BusinessHasEmailFormMapping = mapping(
    "email" -> EmailValidator.mandatoryEmail("error.required", "err.invalidLength", "error.invalid", getProperty("validationMaxLengthEmail").toInt)
  )(BusinessHasEmail.apply)(BusinessHasEmail.unapply)

  val businessHasEmailForm = Form(BusinessHasEmailFormMapping)
}
