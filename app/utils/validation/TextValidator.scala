package utils.validation

import config.AmlsPropertiesReader.getProperty
import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

object TextValidator extends FormValidator {

  def mandatoryText(blankValueMessageKey: String, invalidLengthMessageKey: String,
                    validationMaxLengthProperty: String): Mapping[String] = {
    val constraint = Constraint[String]("Blank and length")({
        case "" => Invalid(blankValueMessageKey)
        case t if t.length > getProperty(validationMaxLengthProperty).toInt => Invalid(invalidLengthMessageKey)
        case _ => Valid
    })
    text.verifying(constraint)
  }

  def mandatoryText(blankValueMessageKey: String): Mapping[String] = {
    val constraint = Constraint[String]("Blank and length")({
      case "" => Invalid(blankValueMessageKey)
      case _ => Valid
    })
    text.verifying(constraint)
  }

}
