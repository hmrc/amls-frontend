package utils.validation

import play.api.data.Mapping
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.Forms.text
import play.api.data.validation.Constraint

object EmailValidator extends FormValidator {

  def mandatoryEmail(blankValueMessageKey: String, invalidLengthMessageKey: String,
                     invalidValueMessageKey: String, maxLength: Int) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String =>
        t match {
          case t if t.length == 0 =>
            Invalid(blankValueMessageKey)
          case t if t.length > maxLength =>
            Invalid(invalidLengthMessageKey)
          case _ =>
            Valid
        }
    } )
    val valueConstraint = Constraint("Value")( {
      t:String => t match {
        case t if t.matches(emailRegex.regex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

}
