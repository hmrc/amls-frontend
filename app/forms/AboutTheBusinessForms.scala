package forms

import models.TelephoningYourBusiness
import play.api.data.Form
import play.api.data.Forms._

object AboutTheBusinessForms {

  val telephoningYourBusinessForm = Form(mapping(
    "businessPhoneNumber" -> nonEmptyText(minLength = 10, maxLength = 30),
    "mobileNumber" -> optional(text).verifying("Invalid Phone Number", _.nonEmpty) //TODO Proper Validation
  )(TelephoningYourBusiness.apply)(TelephoningYourBusiness.unapply))
}

