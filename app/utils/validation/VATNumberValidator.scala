package utils.validation

import config.AmlsPropertiesReader._
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}


object VATNumberValidator extends FormValidator {
  def vatNumber(invalidLengthMessageKey: String,
                  invalidValueMessageKey: String) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String =>
        t match {
          case t if t.length > getProperty("validationMaxLengthVAT").toInt =>
            Invalid(invalidLengthMessageKey)
          case _ =>
            Valid
        }
    } )
    val valueConstraint = Constraint("Value")( {
      t:String => t match {
        case t if t.matches(vatRegex.regex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

}
