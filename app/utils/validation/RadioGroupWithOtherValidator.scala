package utils.validation

import play.api.data.Forms
import play.api.data.format.Formatter
import play.api.data.FormError
import config.AmlsPropertiesReader.getProperty

object RadioGroupWithOtherValidator extends RadioGroupWithOtherValidator

class RadioGroupWithOtherValidator extends FormValidator {

  private def radioGroupWithOtherFormatter(textFieldKey:String,
                                           noRadioButtonSelectedMessageKey: String,
                                           blankValueMessageKey: String,
                                           invalidLengthMessageKey: String,
                                            maxLengthKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          n.trim match {
            case p if p.length==0 => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
            case p if p=="Other" =>
              data.getOrElse(textFieldKey, "").trim match {
                case q if q.length == 0 => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
                case q if q.length > getProperty(maxLengthKey).toInt => Left(Seq(FormError(textFieldKey, invalidLengthMessageKey)))
                case _ => Right(n)
              }
            case _ => Right(n)
            }
        case _ => Left(Seq(FormError(key, "Nothing to validate")))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  def radioGroupWithOther( textFieldKey:String, noRadioButtonSelectedMessageKey: String,
                     blankValueMessageKey: String,
                     invalidLengthMessageKey: String, maxLengthKey: String) =
    Forms.of[String](radioGroupWithOtherFormatter(textFieldKey, noRadioButtonSelectedMessageKey,
      blankValueMessageKey, invalidLengthMessageKey, maxLengthKey))
}
