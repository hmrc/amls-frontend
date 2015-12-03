package utils.validation

import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}


object WebAddressValidator extends FormValidator {
  def webAddress(invalidLengthMessageKey: String,
                 invalidValueMessageKey: String,
                 maxLengthWebAddress: Int): Mapping[String] = {
    val blankConstraint = Constraint("Blank")({
      t: String =>
        t match {
          case t if t.length > maxLengthWebAddress => Invalid(invalidLengthMessageKey)
          case _ => Valid
        }
    })
    val valueConstraint = Constraint("Value")({
      t: String => t match {
        case t if t.matches(webAddressRegex.regex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    })
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

}
