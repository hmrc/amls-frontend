package utils.validation

import play.api.data.Forms
import play.api.data.format.Formatter
import play.api.data.FormError

object BooleanWithTextValidator extends FormValidator {

  private def mandatoryBooleanWithTextFormatter(textFieldKey:String, booleanValueThatMandatesTextValue:String,
                                           noRadioButtonSelectedMessageKey: String,
                                           blankValueMessageKey: String,
                                           notBlankValueMessageKey: String) = new Formatter[Boolean] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] = {
      data.get(key) match {
        case Some(n) =>
          n.trim match {
            case p if p.length==0 => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
            case p if p == booleanValueThatMandatesTextValue =>
              data.getOrElse(textFieldKey, "") match {
                case q if q.length == 0 => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
                case _ => Right(n.toBoolean)
              }
            case p if p != booleanValueThatMandatesTextValue  =>
              data.getOrElse(textFieldKey, "") match {
                case q if q.length > 0 => Left(Seq(FormError(textFieldKey, notBlankValueMessageKey)))
                case _ => Right(n.toBoolean)
              }
            //case _ => Right(n.toBoolean)
            }
        case _ => {
          Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
        }
      }
    }

    override def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
  }

  def mandatoryBooleanWithText( textFieldKey:String, booleanValueThatMandatesTextValue: String,
                                noRadioButtonSelectedMessageKey: String,
                                blankValueMessageKey: String, notBlankValueMessageKey: String) =
    Forms.of[Boolean](mandatoryBooleanWithTextFormatter(textFieldKey, booleanValueThatMandatesTextValue,
      noRadioButtonSelectedMessageKey,
      blankValueMessageKey, notBlankValueMessageKey: String))
}
