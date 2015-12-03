package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms}

object RadioGroupWithOtherValidator extends FormValidator {

  def radioGroupWithOther(textFieldKey: String, otherValue: String, noRadioButtonSelectedMessageKey: String,
                          blankValueMessageKey: String,
                          invalidLengthMessageKey: String, maxLength: Integer) =
    Forms.of[String](radioGroupWithOtherFormatter(textFieldKey, otherValue, noRadioButtonSelectedMessageKey,
      blankValueMessageKey, invalidLengthMessageKey, maxLength))

  private def radioGroupWithOtherFormatter(textFieldKey: String, otherValue: String,
                                           noRadioButtonSelectedMessageKey: String,
                                           blankValueMessageKey: String,
                                           invalidLengthMessageKey: String,
                                           maxLength: Integer) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          n.trim match {
            case p if p.length == 0 => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
            case p if p == otherValue =>
              data.getOrElse(textFieldKey, "") match {
                case q if q.length == 0 => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
                case q if q.length > maxLength => Left(Seq(FormError(textFieldKey, invalidLengthMessageKey)))
                case _ => Right(n)
              }
            case _ => Right(n)
          }
        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }
}
