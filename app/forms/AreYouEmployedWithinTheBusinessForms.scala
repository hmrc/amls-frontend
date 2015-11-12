package forms

import models.AreYouEmployedWithinTheBusinessModel
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import utils.validation.TextValidator

object AreYouEmployedWithinTheBusinessForms {

  val areYouEmployedWithinTheBusinessFormMapping = mapping(
    "radio_inline_group_2" -> TextValidator.mandatoryText(Messages("areYouEmployedWithinTheBusiness.err.YesNo.notsupplied"),
                                                          Messages("err.invalidLength"),
                                                          "yesNoRadioButtonText")
  )(AreYouEmployedWithinTheBusinessModel.apply)(AreYouEmployedWithinTheBusinessModel.unapply)

  val areYouEmployedWithinTheBusinessForm = Form(areYouEmployedWithinTheBusinessFormMapping)

}
