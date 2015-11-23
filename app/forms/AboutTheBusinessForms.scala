package forms

import models.TelephoningBusiness
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages

object AboutTheBusinessForms {

  val telephoningBusinessForm = Form(mapping(
    "businessPhoneNumber" -> (nonEmptyText(minLength = 10, maxLength = 30) //TODO Proper Validation
      verifying(Messages("telephoningbusiness.error.invalidphonenumber"), _.trim.length >= 10)),
    "mobileNumber" -> optional(text).verifying(Messages("telephoningbusiness.error.invalidmobilenumber"),
      mob => mob.exists(_.trim.length >= 10)) //TODO Proper Validation
  )(TelephoningBusiness.apply)(TelephoningBusiness.unapply))
}

