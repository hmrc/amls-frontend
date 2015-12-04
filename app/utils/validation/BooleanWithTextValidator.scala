package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms}

object BooleanWithTextValidator extends FormValidator {

  def mandatoryBooleanWithText(fieldKey: String, booleanValueThatMandatesTextValue: String,
                               noRadioButtonSelectedMessageKey: String,
                               blankValueMessageKey: String, notBlankValueMessageKey: String) =
    Forms.of[Boolean](mandatoryBooleanWithTextFormatter(fieldKey, booleanValueThatMandatesTextValue,
      noRadioButtonSelectedMessageKey,
      blankValueMessageKey, notBlankValueMessageKey: String))

  private def mandatoryBooleanWithTextFormatter(fieldKey: String, booleanValueThatMandatesTextValue: String,
                                                noRadioButtonSelectedMessageKey: String,
                                                blankValueMessageKey: String,
                                                notBlankValueMessageKey: String) = new Formatter[Boolean] {
    override def bind(key: String, formData: Map[String, String]): Either[Seq[FormError], Boolean] = {
      formData.get(key) match {
        case Some(fieldValue) => validateFieldValue(fieldValue)
        case _ => {
          Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
        }
      }


      def validateFieldValue(fieldValue: String) = {
        fieldValue.trim match {
          case p if p.length == 0 => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
          case _ => {
            validationHelper({ (a: String, b: String) => a == b }, { (x: Int) => x == 0 }, fieldKey)
            validationHelper({ (a: String, b: String) => a != b }, { (x: Int) => x > 0 }, fieldKey)
          }
        }

      def validationHelper(func1: (String, String) => Boolean, func2: (Int => Boolean), textFieldKey: String) = {
        if (func1(fieldValue.trim, booleanValueThatMandatesTextValue))
          checkTextForTheRadioSelection(func2, fieldValue, fieldKey)
        else
          Left(Seq(FormError(key, noRadioButtonSelectedMessageKey))) //TODO
      }

      }
      def checkTextForTheRadioSelection(func: (Int => Boolean), fieldValue: String, textFieldKey: String): Either[Seq[FormError], Boolean] with Product with Serializable = {
        formData.getOrElse(textFieldKey, "") match {
          case q if func(q.length) => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
          case _ => Right(fieldValue.toBoolean)
        }

      }
    }

    override def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
  }
}
