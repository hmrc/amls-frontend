package forms

import config.AmlsPropertiesReader._
import models._
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.BooleanWithTextValidator._
import utils.validation.PhoneNumberValidator._
import utils.validation.BooleanTupleValidator._
import utils.validation.{EmailValidator, NumberValidator, RadioGroupPrefRegForMLRWithTextValidator, WebAddressValidator}

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

  val registeredOfficeForm = Form(mapping(
    "isRegisteredOffice" -> mandatoryBooleanTuple(StringToBooleanTupleMappings123ToTTTFFF)
  )(RegisteredOffice.fromBooleanTuple)(RegisteredOffice.toBooleanTuple))

  val businessRegForVATForm = Form(mapping(
    "hasVAT" -> mandatoryBooleanWithText("VATNum", "true", "err.required", "err.required", "err.notrequired"),
    "VATNum" -> optional(NumberValidator.validateNumber("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthVAT"), getIntFromProperty("validationMaxLengthVAT")))
  )(BusinessWithVAT.apply)(BusinessWithVAT.unapply))

  val businessHasEmailForm = Form(mapping(
    "email" -> EmailValidator.mandatoryEmail("err.required", "err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthEmail"))
  )(BusinessHasEmail.apply)(BusinessHasEmail.unapply))

  val RegisteredForMLRForm = Form(mapping(
    "hasMLR" -> RadioGroupPrefRegForMLRWithTextValidator.mandatoryBooleanWithText("mlrNumber",
      "prevMlrNumber", "err.required", "err.required", "err.notrequired"),
    "mlrNumber" -> optional(NumberValidator.validateNumber("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthMLR"), getIntFromProperty("validationMaxLengthMLR"))),
    "prevMlrNumber" -> optional(NumberValidator.validateNumber("err.invalidLength", "err.invalid",
      getIntFromProperty("validationMaxLengthMLR"), getIntFromProperty("validationMaxLengthPrevMLR")))
  )(applyRegisteredForMLRFormMapping)(unapplyRegisteredForMLRFormMapping))

  def applyRegisteredForMLRFormMapping = (hasMlr: (Boolean, Boolean), mlrNumber: Option[String],
                                          prevMlrNumber: Option[String]) =>
    RegisteredForMLR(hasMlr._1, hasMlr._2, mlrNumber, prevMlrNumber)

  def unapplyRegisteredForMLRFormMapping = (registeredForMLR: RegisteredForMLR) =>
    Some(((registeredForMLR.hasDigitalMLR, registeredForMLR.hasNonDigitalMLR), registeredForMLR.mlrNumber,
      registeredForMLR.prevMlrNumber))
}
