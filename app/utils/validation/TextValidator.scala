package utils.validation

import play.api.data.Mapping
import play.api.data.validation.Invalid
import config.AmlsPropertiesReader.getProperty
import play.api.data.Forms.text
import play.api.data.validation.Constraint
import play.api.data.validation.Valid

object TextValidator extends FormValidator {

  def mandatoryText(blankValueMessageKey:String, invalidLengthMessageKey: String,
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

  def mandatoryText(blankValueMessageKey:String): Mapping[String] = {
    val constraint = Constraint("Blank and length")( {
      t:String =>
        t match {
          case t if t.length == 0 => Invalid(blankValueMessageKey)
          case _ => Valid
        }
    } )
    text.verifying(constraint)
  }

}
