package utils.validation

import play.api.data.Forms._
import play.api.data.Mapping
import play.api.data.validation.{Constraint, Invalid, Valid}


object NumberValidator extends FormValidator {
  def validateNumber(invalidLengthMessageKey: String,
                  invalidValueMessageKey: String, minLength:Int, maxLength:Int) : Mapping[String] = {
    val lengthConstraint = Constraint("Length")( {
      t:String =>
        t match {
          case t if t.length == maxLength || t.length == minLength  =>
            Valid
          case _ =>
            Invalid(invalidLengthMessageKey)
        }
    } )
    val valueConstraint = Constraint("Value")( {
      t:String =>
        numberRegex.findFirstIn(t) match {
          case Some(x) => Valid
          case _ => Invalid(invalidValueMessageKey)
        }
    } )
    text.verifying(stopOnFirstFail(valueConstraint, lengthConstraint))
  }

}
