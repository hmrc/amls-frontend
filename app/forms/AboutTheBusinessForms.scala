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
  def applyRegisteredForMLRFormMapping = (hasMlr:(Boolean, Boolean), mlrNumber: Option[String],
                                          prevMlrNumber: Option[String]) =>
    RegisteredForMLR(hasMlr._1, hasMlr._2, mlrNumber, prevMlrNumber)

  def unapplyRegisteredForMLRFormMapping = (registeredForMLR: RegisteredForMLR) =>
    Some( ( (registeredForMLR.hasDigitalMLR, registeredForMLR.hasNonDigitalMLR), registeredForMLR.mlrNumber,
      registeredForMLR.prevMlrNumber ) )

  val businessHasWebsiteFormMapping = mapping(
    "hasWebsite" -> mandatoryBooleanWithText("website", "true",
      "error.required", "error.required", "error.notrequired"),
    "website" -> optional(WebAddressValidator.webAddress("err.invalidLength", "error.invalid",
      getIntFromProperty("validationMaxLengthWebAddress")))
  )(BusinessHasWebsite.apply)(BusinessHasWebsite.unapply)

  val businessHasWebsiteForm = Form(businessHasWebsiteFormMapping)

  val telephoningBusinessForm = Form(mapping(
    "businessPhoneNumber" -> mandatoryPhoneNumber("error.required", "err.invalidLength",
      "telephoningbusiness.error.invalidphonenumber", getIntFromProperty("validationMaxLengthPhoneNo")),
    "mobileNumber" -> optional(mandatoryPhoneNumber("error.required", "err.invalidLength",
      "telephoningbusiness.error.invalidphonenumber", getIntFromProperty("validationMaxLengthPhoneNo")))
  )(TelephoningBusiness.apply)(TelephoningBusiness.unapply))

  def applyToModel(tuple: (Boolean, Boolean)): RegisteredOffice = {
    RegisteredOffice(tuple._1, tuple._2)
  }
  def unapplyFromModel(registeredOffice: RegisteredOffice): Some[(Boolean, Boolean)] = {
    Some(Tuple2( registeredOffice.isRegisteredOffice, registeredOffice.isCorrespondenceAddressSame ))
  }

  val ValidValuesForRadioButton = Seq("1", "2", "3")
  val TupleMapping = Seq((true, true), (true, false), (false, false))
  val StringToBooleanMapping: Seq[(String, (Boolean, Boolean))] = ValidValuesForRadioButton.zip(TupleMapping)

  val registeredOfficeForm = Form(mapping(
  "isRegisteredOffice" -> mandatoryBooleanTuple(StringToBooleanMapping)
  )(applyToModel)(unapplyFromModel))


  val businessRegForVATFormMapping = mapping(
    "hasVAT" -> mandatoryBooleanWithText("VATNum", "true", "error.required", "error.required", "error.notrequired"),
    "VATNum" -> optional(NumberValidator.validateNumber("err.invalidLength", "error.invalid",
      getIntFromProperty("validationMaxLengthVAT"), getIntFromProperty("validationMaxLengthVAT")))
  )(BusinessWithVAT.apply)(BusinessWithVAT.unapply)

  val businessRegForVATForm = Form(businessRegForVATFormMapping)

  val BusinessHasEmailFormMapping = mapping(
    "email" -> EmailValidator.mandatoryEmail("error.required", "err.invalidLength", "error.invalid",
      getIntFromProperty("validationMaxLengthEmail"))
  )(BusinessHasEmail.apply)(BusinessHasEmail.unapply)

  val businessHasEmailForm = Form(BusinessHasEmailFormMapping)

  val RegisteredForMLRFormMapping = mapping(
    "hasMLR" ->  RadioGroupPrefRegForMLRWithTextValidator.mandatoryBooleanWithText("mlrNumber",
      "prevMlrNumber", "error.required", "error.required", "error.notrequired"),
    "mlrNumber" -> optional(NumberValidator.validateNumber("err.invalidLength", "error.invalid",
      getIntFromProperty("validationMaxLengthMLR"), getIntFromProperty("validationMaxLengthMLR"))),

    "prevMlrNumber" -> optional(NumberValidator.validateNumber("err.invalidLength", "error.invalid",
      getIntFromProperty("validationMaxLengthMLR"), getIntFromProperty("validationMaxLengthPrevMLR")))
  )(applyRegisteredForMLRFormMapping )(unapplyRegisteredForMLRFormMapping)

  val RegisteredForMLRForm = Form(RegisteredForMLRFormMapping)
}
