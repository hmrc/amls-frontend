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
    "roleWithinBusiness" -> nonEmptyText
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply)

  val roleWithinBusinessForm = Form(roleWithinBusinessFormMapping)

  val roleForBusinessFormMapping = mapping(
    "roleForBusiness" -> nonEmptyText,
    "other" -> text
  )(RoleForBusiness.apply)(RoleForBusiness.unapply)

  val roleForBusinessForm = Form(roleForBusinessFormMapping)
}
