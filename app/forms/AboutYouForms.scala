package forms

import config.AmlsPropertiesReader._
import models._
import play.api.data.Forms._
import play.api.data._
import utils.validation.BooleanValidator._
import utils.validation.RadioGroupWithOtherValidator.radioGroupWithOther
import utils.validation.TextValidator

object AboutYouForms {

  val yourNameForm = Form(mapping(
    "firstname" -> TextValidator.mandatoryText("err.titleNotEntered.first_name",
      "err.invalidLength", getIntFromProperty("validationMaxLengthFirstName")),
    "middlename" -> optional(TextValidator.mandatoryText("", "err.invalidLength",
      getIntFromProperty("validationMaxLengthFirstName"))),
    "lastname" -> TextValidator.mandatoryText("err.titleNotEntered.last_name", "err.invalidLength",
      getIntFromProperty("validationMaxLengthFirstName"))
  )(YourName.apply)(YourName.unapply))

  val employedWithinTheBusinessForm = Form(mapping(
    "isEmployed" -> mandatoryBoolean("error.required")
  )(EmployedWithinTheBusiness.apply)(EmployedWithinTheBusiness.unapply))

  val roleWithinBusinessForm = Form(mapping(
    "roleWithinBusiness" -> radioGroupWithOther("other", getProperty("roleWithinBusiness").split(",").reverse.head,
      "error.required", "error.required", "error.invalid", getIntFromProperty("validationMaxLengthRoleWithinBusinessOther")),
    "other" -> text
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply))


  val roleForBusinessForm = Form(mapping(
    "roleForBusiness" -> radioGroupWithOther("other", getProperty("roleForBusiness").split(",").reverse.head,
      "error.required", "error.required", "error.invalid", getIntFromProperty("validationMaxLengthRoleForBusinessOther")),
    "other" -> text
  )(RoleForBusiness.apply)(RoleForBusiness.unapply))

}
