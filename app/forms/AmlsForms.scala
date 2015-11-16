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
}
