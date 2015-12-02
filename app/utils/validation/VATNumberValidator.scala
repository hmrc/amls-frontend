package utils.validation

import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}


object VATNumberValidator extends FormValidator {
  def vatNumber(invalidLengthMessageKey: String,
                invalidValueMessageKey: String,
                maxLengthVAT: Int): Mapping[String] = {
    val blankConstraint = Constraint("Blank")({
      t: String =>
        t match {
          case t if t.length > maxLengthVAT => Invalid(invalidLengthMessageKey)
          case _ => Valid
        }
    })
    val valueConstraint = Constraint("Value")({
      t: String => t match {
        case t if t.matches(vatRegex.regex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    })
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

}
