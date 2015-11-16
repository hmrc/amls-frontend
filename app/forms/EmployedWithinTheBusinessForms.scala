package forms

import models.AreYouEmployedWithinTheBusinessModel
import play.api.data.Form
import play.api.data.Forms._

object EmployedWithinTheBusinessForms {

  val areYouEmployedWithinTheBusinessFormMapping = mapping(
    "isEmployed" -> boolean
  )(AreYouEmployedWithinTheBusinessModel.apply)(AreYouEmployedWithinTheBusinessModel.unapply)

  val areYouEmployedWithinTheBusinessForm = Form(areYouEmployedWithinTheBusinessFormMapping)

}
