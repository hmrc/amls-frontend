package forms

import models._

import play.api.data.Forms._
import play.api.data._

object AmlsForms {

  val loginDetailsFormMapping = mapping(
    "name" -> text,
    "password" -> text
  )(LoginDetails.apply)(LoginDetails.unapply)

  val loginDetailsForm = Form(loginDetailsFormMapping)

  val yourNameFormMapping = mapping (
  "firtname" -> text(maxLength = 35).verifying("charities_err.titleNotEntered", model => model.nonEmpty),
  "lastname" -> text(maxLength = 35).verifying("charities_err.titleNotEntered", model => model.nonEmpty),
  "middlename" -> text(maxLength = 35)
  )(YourName.apply)(YourName.unapply)

  val yourNameForm = Form(yourNameFormMapping)

}


