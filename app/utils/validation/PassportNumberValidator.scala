package utils.validation

import play.api.data.Mapping
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.Forms.text
import play.api.data.validation.Constraint
import config.AmlsPropertiesReader.getProperty

object PassportNumberValidator extends PassportNumberValidator

class PassportNumberValidator extends FormValidator {

  def mandatoryPassportNumber(blankValueMessageKey: String, invalidLengthMessageKey: String,
                     invalidValueMessageKey: String) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String =>
        t match {
          case t if t.length == 0 =>
            Invalid(blankValueMessageKey)
          case t if t.length > getProperty("validationMaxLengthPassportNumber").toInt =>
            Invalid(invalidLengthMessageKey)
          case _ =>
            Valid
        }
    } )
    val valueConstraint = Constraint("Value")( {
      t:String => t match {
        case t if t.matches(ukPassportNumberRegex.regex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

}
