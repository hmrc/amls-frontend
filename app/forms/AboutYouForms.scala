package forms

import config.AmlsPropertiesReader._
import models._
import play.api.data.Forms._
import play.api.data._
import utils.validation.BooleanValidator._
import utils.validation.RadioGroupWithOtherValidator.radioGroupWithOther
import utils.validation.TextValidator

object AboutYouForms {

  val yourNameFormMapping = mapping (
  "firstname" -> TextValidator.mandatoryText("err.titleNotEntered.first_name",
                                              "err.invalidLength","validationMaxLengthYourNameFirstName"),
  "middlename" -> optional(TextValidator.mandatoryText("",
                                                        "err.invalidLength","validationMaxLengthYourNameFirstName")),
  "lastname" -> TextValidator.mandatoryText("err.titleNotEntered.last_name",
                                              "err.invalidLength","validationMaxLengthYourNameFirstName")

  )(YourName.apply)(YourName.unapply)

  val yourNameForm = Form(yourNameFormMapping)

  val employedWithTheBusinessFormMapping = mapping (
    "isEmployed" -> mandatoryBoolean("error.boolean.notsupplied")
  )(EmployedWithinTheBusiness.apply)(EmployedWithinTheBusiness.unapply)

  val employedWithinTheBusinessForm = Form(employedWithTheBusinessFormMapping)

  val roleWithinBusinessFormMapping = mapping(
    "roleWithinBusiness" -> radioGroupWithOther("other", getProperty("roleWithinBusiness").split(",").reverse.head,
      "error.required", "error.required", "error.invalid", getIntFromProperty("validationMaxLengthRoleWithinBusinessOther")),
    "other" -> text
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply)

  val roleWithinBusinessForm = Form(roleWithinBusinessFormMapping)

  val roleForBusinessFormMapping = mapping(
    "roleForBusiness" -> radioGroupWithOther("other", getProperty("roleForBusiness").split(",").reverse.head,
      "error.required", "error.required", "error.invalid", getIntFromProperty("validationMaxLengthRoleForBusinessOther")),
    "other" -> text
  )(RoleForBusiness.apply)(RoleForBusiness.unapply)

  val roleForBusinessForm = Form(roleForBusinessFormMapping)

}
