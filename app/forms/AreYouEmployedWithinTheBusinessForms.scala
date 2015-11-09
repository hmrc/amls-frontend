package forms

import models.AreYouEmployedWithinTheBusinessModel
import play.api.data.Form
import play.api.data.Forms._

object AreYouEmployedWithinTheBusinessForms {

  val areYouEmployedWithinTheBusinessFormMapping = mapping(
    "radio_inline_group_2" -> text
  )(AreYouEmployedWithinTheBusinessModel.apply)(AreYouEmployedWithinTheBusinessModel.unapply)

  val areYouEmployedWithinTheBusinessForm = Form(areYouEmployedWithinTheBusinessFormMapping)

}
