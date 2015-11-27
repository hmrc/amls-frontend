package utils.validation

import play.api.data.Forms
import play.api.data.format.Formatter
import play.api.data.FormError
import config.AmlsPropertiesReader.getProperty

object PhoneNumberValidator extends FormValidator {

  private def mandatoryPhoneNumberFormatter(blankValueMessageKey: String,
                                            invalidLengthMessageKey: String,
                                            invalidValueMessageKey: String,
                                            maxLengthForPhone : String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          val s = n.trim.toUpperCase
          val t = if (s.length > 0 && s.charAt(0)=='+') "00" + s.substring(1) else s
          t match{
            case p if p.length==0 => Left(Seq(FormError(key, blankValueMessageKey)))
            case num => {
              if (num.trim.length > getProperty(maxLengthForPhone).toInt) {
                Left(Seq(FormError(key, invalidLengthMessageKey)))
              } else {
                phoneNoRegex.findFirstIn(num) match {
                  case None => Left(Seq(FormError(key, invalidValueMessageKey)))
                  case _ => Right(num)
                }
              }
            }
          }

        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }
    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  def mandatoryPhoneNumber( blankValueMessageKey: String,
                            invalidLengthMessageKey: String,
                            invalidValueMessageKey: String,
                            maxLengthForPhone : String ) =
    Forms.of[String](mandatoryPhoneNumberFormatter(blankValueMessageKey,
      invalidLengthMessageKey, invalidValueMessageKey, maxLengthForPhone))
}
