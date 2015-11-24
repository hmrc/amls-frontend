package forms

import models.TelephoningBusiness
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.PhoneNumberValidator._

object AboutTheBusinessForms {

  val telephoningBusinessForm = Form(mapping(
    "businessPhoneNumber" -> mandatoryPhoneNumber("error.required", "err.invalidLength", "telephoningbusiness.error.invalidphonenumber"),
    "mobileNumber" -> optional(mandatoryPhoneNumber("error.required", "err.invalidLength", "telephoningbusiness.error.invalidphonenumber"))
  )(TelephoningBusiness.apply)(TelephoningBusiness.unapply))
}

