package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms}

object PhoneNumberValidator extends FormValidator {

  def mandatoryPhoneNumber(blankValueKey: String,
                           invalidLengthKey: String,
                           invalidValueKey: String,
                           maxLengthPhoneNumber: Int) =
    Forms.of[String](mandatoryPhoneNumberFormatter(blankValueKey,
      invalidLengthKey, invalidValueKey, maxLengthPhoneNumber))

  private def mandatoryPhoneNumberFormatter(blankValueKey: String,
                                            invalidLengthKey: String,
                                            invalidValueKey: String,
                                            maxLengthPhoneNumber: Int) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          val s = n.trim.toUpperCase
          val t = if (s.length > 0 && s.charAt(0) == '+') "00" + s.substring(1) else s
          t.trim match {
            case "" => Left(Seq(FormError(key, blankValueKey)))
            case num => {
              if (num.length > maxLengthPhoneNumber) {
                Left(Seq(FormError(key, invalidLengthKey)))
              } else {
                phoneNoRegex.findFirstIn(num) match {
                  case None => Left(Seq(FormError(key, invalidValueKey)))
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
}
