package utils.validation

import play.api.data.{Forms, Mapping}
import play.api.data.format.Formatter
import play.api.data.Forms.text
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.FormError

object BankDetailsValidator extends BankDetailsValidator

class BankDetailsValidator extends FormValidator {

  def mandatoryAccountNumber(emptyMessageKey: String, invalidMessageKey: String): Mapping[String] = {
    val constraint = Constraint("Blank and invalid")( {
      t: String => t match {
        case "" => Invalid(emptyMessageKey)
        case x if !x.matches(accountNumberRegex.regex) => Invalid(invalidMessageKey)
        case _ => Valid
      }
    } )
    text.verifying(constraint)
  }

  def mandatorySortCode(emptyMessageKey: String, invalidMessageKey: String): Mapping[String] = {
    val constraint = Constraint("Blank and invalid")( {
      t: String => t match {
        case "" => Invalid(emptyMessageKey)
        case x if !x.matches(sortCodeRegex.regex) => Invalid(invalidMessageKey)
        case _ => Valid
      }
    } )
    text.verifying(constraint)
  }

  private def mandatoryIbanFormatter(blankValueMessageKey: String, invalidValueMessageKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          val s = n.trim.replaceAll(" ", "")
          s match{
            case p if p.length==0 => Left(Seq(FormError(key, blankValueMessageKey)))
            case num => {
              ibanRegex.findFirstIn(num) match {
                case None => Left(Seq(FormError(key, invalidValueMessageKey)))
                case _ => Right(num)
              }
            }
          }
        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }
    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  def mandatoryIban(blankValueMessageKey: String, invalidValueMessageKey: String) =
    Forms.of[String](mandatoryIbanFormatter(blankValueMessageKey, invalidValueMessageKey))

}
