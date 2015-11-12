package forms

import models._
import play.api.data.Forms._
import play.api.data._

object AboutYouForms {

  val yourNameFormMapping = mapping (
  "firtname" -> text(maxLength = 35).verifying("charities_err.titleNotEntered", model => model.nonEmpty),
  "lastname" -> text(maxLength = 35).verifying("charities_err.titleNotEntered", model => model.nonEmpty),
  "middlename" -> text(maxLength = 35)
  )(YourName.apply)(YourName.unapply)

  val yourNameForm = Form(yourNameFormMapping)

  val roleWithinBusinessFormMapping = mapping(
    "roleWithinBusiness" -> nonEmptyText,
    "other" -> text
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply)

  val roleWithinBusinessForm = Form(roleWithinBusinessFormMapping)
}
