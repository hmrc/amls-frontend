package forms

import models._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.Messages
import utils.validation.TextValidator

object AboutYouForms {

  val yourNameFormMapping = mapping (
  "firtname" -> TextValidator.mandatoryText(Messages("err.titleNotEntered.first_name"),Messages("err.invalidLength"),"validationMaxLengthFirstName"),
  "middlename" -> optional(TextValidator.mandatoryText(Messages(""),Messages("err.invalidLength"),"validationMaxLengthFirstName")),
  "lastname" -> TextValidator.mandatoryText(Messages("err.titleNotEntered.last_name"),Messages("err.invalidLength"),"validationMaxLengthFirstName")

  )(YourName.apply)(YourName.unapply)

  val yourNameForm = Form(yourNameFormMapping)

}