package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms}

object BooleanWithTextValidator extends FormValidator {

  private def mandatoryBooleanWithTextFormatter(textFieldKey: String, booleanValue: String,
                                                noRadioButtonSelectedKey: String,
                                                blankValueKey: String,
                                                notBlankValueKey: String) = new Formatter[Boolean] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] = {
      data.get(key).fold[Either[Seq[FormError], Boolean]]{
        Left(Seq(FormError(key, noRadioButtonSelectedKey)))
      } { _.trim match {
          case "" => Left(Seq(FormError(key, noRadioButtonSelectedKey)))
          case p if p == booleanValue => checkTextField(_.trim == "", data.getOrElse(textFieldKey, "") , p, textFieldKey, blankValueKey)
          case p if p != booleanValue => checkTextField(_.length > 0, data.getOrElse(textFieldKey, ""), p, textFieldKey, notBlankValueKey)
        }
      }
    }

    override def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
  }

  private def checkTextField(shouldTextBePresent: String => Boolean, data: String, n: String, textFieldKey: String, messageKey: String) = {
    data match {
      case textValue if shouldTextBePresent(textValue) => Left(Seq(FormError(textFieldKey, messageKey)))
      case _ => Right(n.toBoolean)
    }
  }

  def mandatoryBooleanWithText(textFieldKey: String, booleanValue: String,
                               noRadioButtonSelectedMessageKey: String,
                               blankValueMessageKey: String, notBlankValueMessageKey: String) =
    Forms.of[Boolean](mandatoryBooleanWithTextFormatter(textFieldKey, booleanValue,
      noRadioButtonSelectedMessageKey,
      blankValueMessageKey, notBlankValueMessageKey: String))
}
