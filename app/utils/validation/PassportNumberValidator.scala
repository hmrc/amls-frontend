package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms}

import scala.util.matching.Regex

object PassportNumberValidator extends FormValidator {

  def mandatoryPassportNumber(isUkPassportKey: String,
                              blankValueKey: String,
                              invalidLengthKey: String,
                              invalidValueKey: String,
                              passportNumberLengths: Int*) = //NewUKPassportNumber,OldUKPassportNumber,MinNonUKPassportNumber,MaxNonUKPassportNumber
    Forms.of[String](mandatoryPassportNumberFormatter(isUkPassportKey, blankValueKey,
      invalidLengthKey, invalidValueKey, passportNumberLengths: _*))

  private def mandatoryPassportNumberFormatter(isUkPassportKey: String, blankValueKey: String,
                                               invalidLengthKey: String, invalidValueKey: String,
                                               passportNumberLengths: Int*): Formatter[String] = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      val isUkPassport = data.getOrElse(isUkPassportKey, "false").toBoolean
      data.get(key) match {
        case Some(passportNumber) =>
          val trimmedPassportNumber = passportNumber.trim.replaceAll(" ", "")
          trimmedPassportNumber match {
            case "" => Left(Seq(FormError(key, blankValueKey)))
            case num if isUkPassport => checkPassport((num, pp) => num.length != pp.head && num.length != pp(1),
              ukPassportNumberRegex, num, key)
            case num if !isUkPassport => checkPassport((num, pp) => num.length < pp(2) || num.length > pp(3),
              nonUkPassportNumberRegex, num, key)
          }
        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)

    def checkPassport(checkLength: (String, Seq[Int]) => Boolean, passportRegex: Regex, num: String, key: String): Either[Seq[FormError], String] = {
      if (checkLength(num, passportNumberLengths)) {
        Left(Seq(FormError(key, invalidLengthKey)))
      } else {
        passportRegex.findFirstIn(num) match {
          case None => Left(Seq(FormError(key, invalidValueKey)))
          case _ => Right(num)
        }
      }
    }

  }


}
