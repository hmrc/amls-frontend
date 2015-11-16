package utils.validation

import play.api.data.{Forms, FormError, Mapping}
import play.api.data.format.Formatter
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.Forms.text
import play.api.data.validation.Constraint
import config.AmlsPropertiesReader.getProperty

object PassportNumberValidator extends PassportNumberValidator

class PassportNumberValidator extends FormValidator {

  private def mandatoryPassportNumberFormatter(isUkPassportKey: String, blankValueMessageKey: String, invalidLengthMessageKey: String,
                                     invalidValueMessageKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      val isUkPassport = data.get(isUkPassportKey) match {
        case Some("true") => true
        case _ => false
      }
      data.get(key) match {
        case Some(passportNumber) =>
          val trimmedPassportNumber = passportNumber.trim.replaceAll(" ", "")
          trimmedPassportNumber match{
            case p if p.length==0 => Left(Seq(FormError(key, blankValueMessageKey)))
            case num if isUkPassport => {
              if (num.length != getProperty("validationLengthUkPassportNumber").toInt && num.length !=
                getProperty("validationLengthOldUkPassportNumber").toInt) {
                Left(Seq(FormError(key, invalidLengthMessageKey)))
              } else {
                ukPassportNumberRegex.findFirstIn(num) match {
                  case None => Left(Seq(FormError(key, invalidValueMessageKey)))
                  case _ => Right(num)
                }
              }
            }
            case num if !isUkPassport => {
              if (num.length > getProperty("validationMaxLengthNonUkPassportNumber").toInt || num.length <
                getProperty("validationMinLengthNonUkPassportNumber").toInt) {
                Left(Seq(FormError(key, invalidLengthMessageKey)))
              } else {
                nonUkPassportNumberRegex.findFirstIn(num) match {
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

  def mandatoryPassportNumber(isUkPassportKey: String,
                              blankValueMessageKey: String,
                              invalidLengthMessageKey: String,
                              invalidValueMessageKey: String) =
    Forms.of[String](mandatoryPassportNumberFormatter(isUkPassportKey, blankValueMessageKey,
      invalidLengthMessageKey, invalidValueMessageKey))

}