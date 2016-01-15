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

}
