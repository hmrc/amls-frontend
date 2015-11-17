package forms

import models.EmployedWithinTheBusinessModel
import play.api.data.Form
import play.api.data.Forms._

object EmployedWithinTheBusinessForms {

  val employedWithinTheBusinessFormMapping = mapping(
    "isEmployed" -> boolean
  )(EmployedWithinTheBusinessModel.apply)(EmployedWithinTheBusinessModel.unapply)

  val employedWithinTheBusinessForm = Form(employedWithinTheBusinessFormMapping)

}
