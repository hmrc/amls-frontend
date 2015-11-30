package utils.validation

import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}


object NumberValidator extends FormValidator {
  def validateNumber(invalidLengthMessageKey: String,
                  invalidValueMessageKey: String, maxLength:Int) : Mapping[String] = {
    val blankConstraint = Constraint("Blank")( {
      t:String =>
        t match {
          case t if t.length > maxLength =>
            Invalid(invalidLengthMessageKey)
          case _ =>
            Valid
        }
    } )
    val valueConstraint = Constraint("Value")( {
      t:String =>
        vatRegex.findFirstIn(t) match {
          case Some(x) => Valid
          case _ => Invalid(invalidValueMessageKey)
        }
    } )
    text.verifying(stopOnFirstFail(blankConstraint, valueConstraint))
  }

}
