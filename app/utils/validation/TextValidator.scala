package utils.validation

import play.api.data.Forms.text
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

object TextValidator extends FormValidator {

  def mandatoryText(blankValueMessageKey: String, invalidLengthMessageKey: String,
                    maxLength: Int): Mapping[String] = {
    val constraint = Constraint[String]("Blank and length")({
        case "" => Invalid(blankValueMessageKey)
        case t if t.length > maxLength => Invalid(invalidLengthMessageKey)
        case _ => Valid
    })
    text.verifying(constraint)
  }

  def mandatoryText(blankValueMessageKey: String): Mapping[String] = {
    val constraint = Constraint[String]("Blank")({
      case "" => Invalid(blankValueMessageKey)
      case _ => Valid
    })
    text.verifying(constraint)
  }

}
