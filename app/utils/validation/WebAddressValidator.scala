package utils.validation

import config.AmlsPropertiesReader._
import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Valid, Invalid, Constraint}

object WebAddressValidator extends WebAddressValidator


class WebAddressValidator extends FormValidator {

  def webAddress(blankValueMessageKey:String, invalidLengthMessageKey: String,
                    validationMaxLengthProperty: String): Mapping[String] = {
    val constraint = Constraint("Blank and length")( {
      t:String =>
        t match {
          case t if t.length == 0 => Invalid(blankValueMessageKey)
          case t if t.length > getProperty(validationMaxLengthProperty).toInt =>
            Invalid(invalidLengthMessageKey)
          case _ => Valid
        }
    } )
    text.verifying(constraint)
  }

}
