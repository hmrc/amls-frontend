package forms

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
    "roleWithinBusiness" -> radioGroupWithOther("other", "error.required", "blank", "invalidlength", "validationMaxLengthRoleWithinBusinessOther"),
    "other" -> text
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply)

  val roleWithinBusinessForm = Form(roleWithinBusinessFormMapping)
}
