package forms

import config.AmlsPropertiesReader._
import models._
import play.api.data.Forms._
import play.api.data._
import utils.validation.RadioGroupWithOtherValidator.radioGroupWithOther
import play.api.i18n.Messages
import utils.validation.TextValidator

object AboutYouForms {

  val yourNameFormMapping = mapping (
  "firtname" -> TextValidator.mandatoryText(Messages("err.titleNotEntered.first_name"),
                                              Messages("err.invalidLength"),"validationMaxLengthYourNameFirstName"),
  "middlename" -> optional(TextValidator.mandatoryText(Messages(""),
    Messages("err.invalidLength"),"validationMaxLengthYourNameFirstName")),
  "lastname" -> TextValidator.mandatoryText(Messages("err.titleNotEntered.last_name"),
                                              Messages("err.invalidLength"),"validationMaxLengthYourNameFirstName")

  )(YourName.apply)(YourName.unapply)

  val yourNameForm = Form(yourNameFormMapping)

  val roleWithinBusinessFormMapping = mapping(
    "roleWithinBusiness" -> radioGroupWithOther("other", getProperty("roleWithinBusiness").split(",").reverse.head,
      "error.required", "error.required", "error.invalid", "validationMaxLengthRoleWithinBusinessOther"),
    "other" -> text
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply)

  val roleWithinBusinessForm = Form(roleWithinBusinessFormMapping)
}
