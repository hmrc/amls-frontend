package forms

import models.AreYouEmployedWithinTheBusinessModel
import play.api.data.Form
import play.api.data.Forms._

object AreYouEmployedWithinTheBusinessForms {

  val areYouEmployedWithinTheBusinessFormMapping = mapping(
    "trueFalse" -> boolean
  )(AreYouEmployedWithinTheBusinessModel.apply)(AreYouEmployedWithinTheBusinessModel.unapply)

  val areYouEmployedWithinTheBusinessForm = Form(areYouEmployedWithinTheBusinessFormMapping)

}
