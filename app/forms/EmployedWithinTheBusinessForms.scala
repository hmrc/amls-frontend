package forms

import models.EmployedWithinTheBusinessModel
import play.api.data.Form
import play.api.data.Forms._
import utils.validation.BooleanValidator._

object EmployedWithinTheBusinessForms {

  val employedWithinTheBusinessFormMapping = mapping(
    "isEmployed" -> mandatoryBoolean("error.boolean.notsupplied")
  )(EmployedWithinTheBusinessModel.apply)(EmployedWithinTheBusinessModel.unapply)

  val employedWithinTheBusinessForm = Form(employedWithinTheBusinessFormMapping)

}
