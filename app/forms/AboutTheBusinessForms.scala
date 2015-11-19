package forms

import models.TelephoningBusiness
import play.api.data.Form
import play.api.data.Forms._

object AboutTheBusinessForms {

  val telephoningBusinessForm = Form(mapping(
    "businessPhoneNumber" -> nonEmptyText(minLength = 10, maxLength = 30),
    "mobileNumber" -> optional(text).verifying("Invalid Phone Number", _.nonEmpty) //TODO Proper Validation
  )(TelephoningBusiness.apply)(TelephoningBusiness.unapply))
}

