package forms

import models._
import play.api.data.Form
import play.api.data.Forms._

object AmlsForms {
  val loginDetailsFormMapping = mapping(
    "name" -> text,
    "password" -> text
  )(LoginDetails.apply)(LoginDetails.unapply)

  val loginDetailsForm = Form(loginDetailsFormMapping)
}
