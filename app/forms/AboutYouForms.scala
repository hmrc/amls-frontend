package forms

import models._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import utils.validation.TextValidator

object AboutYouForms {

  val yourNameFormMapping = mapping (
  "firstname" -> TextValidator.mandatoryText("err.titleNotEntered.first_name",
                                              "err.invalidLength","validationMaxLengthYourNameFirstName"),
  "middlename" -> optional(TextValidator.mandatoryText("",
                                                        "err.invalidLength","validationMaxLengthYourNameFirstName")),
  "lastname" -> TextValidator.mandatoryText("err.titleNotEntered.last_name",
                                              "err.invalidLength","validationMaxLengthYourNameFirstName")

  )(YourName.apply)(YourName.unapply)

  val yourNameForm = Form(yourNameFormMapping)

}