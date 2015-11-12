package forms

import config.AmlsPropertiesReader._
import models._
import play.api.data.Forms._
import play.api.data._
import utils.validation.RadioGroupWithOtherValidator.radioGroupWithOther

object AboutYouForms {

  val yourNameFormMapping = mapping (
  "firtname" -> text(maxLength = 35).verifying("charities_err.titleNotEntered", model => model.nonEmpty),
  "lastname" -> text(maxLength = 35).verifying("charities_err.titleNotEntered", model => model.nonEmpty),
  "middlename" -> text(maxLength = 35)
  )(YourName.apply)(YourName.unapply)

  val yourNameForm = Form(yourNameFormMapping)

  val roleWithinBusinessFormMapping = mapping(
    "roleWithinBusiness" -> radioGroupWithOther("other", getProperty("roleWithinBusiness").split(",").reverse.head, "error.required", "error.required", "error.invalid", "validationMaxLengthRoleWithinBusinessOther"),
    "other" -> text
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply)

  val roleWithinBusinessForm = Form(roleWithinBusinessFormMapping)

  val roleForBusinessFormMapping = mapping(
    "roleForBusiness" -> radioGroupWithOther("other", getProperty("roleForBusiness").split(",").reverse.head, "error.required", "error.required", "error.invalid", "validationMaxLengthRoleForBusinessOther"),
    "other" -> text
  )(RoleForBusiness.apply)(RoleForBusiness.unapply)

  val roleForBusinessForm = Form(roleForBusinessFormMapping)
}
