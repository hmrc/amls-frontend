package forms

import models._

import play.api.data.Forms._
import play.api.data._

/**
 * Created by user on 19/08/15.
 */
object AmlsForms {

  val loginDetailsFormMapping = mapping(
    "name" -> text,
    "password" -> text
  )(LoginDetails.apply)(LoginDetails.unapply)

  val loginDetailsForm = Form(loginDetailsFormMapping)
}

