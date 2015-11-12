package utils.validation

import config.AmlsPropertiesReader._
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Valid, Invalid, Constraint}

object WebAddressValidator extends WebAddressValidator


class WebAddressValidator extends FormValidator {
  def webAddress(invalidLengthMessageKey: String,
                  invalidValueMessageKey: String) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String =>
        t match {
          case t if t.length > getProperty("validationMaxLengthWebAddress").toInt =>
            Invalid(invalidLengthMessageKey)
          case _ =>
            Valid
        }
    } )
    val valueConstraint = Constraint("Value")( {
      t:String => t match {
        case t if t.matches(webAddressRegex.regex) => Valid
        case _ => Invalid(invalidValueMessageKey)
      }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

}
