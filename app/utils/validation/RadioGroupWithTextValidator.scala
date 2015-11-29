package utils.validation

import play.api.data.{FormError, Forms}
import play.api.data.format.Formatter

object RadioGroupWithTextValidator extends FormValidator {
  private def mandatoryRadioGroupWithTextFormatter(textFieldKey:String, noRadioButtonSelectedMessageKey:String,
                                                   blankValueMessageKey:String) = new Formatter[Boolean] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] = {
      data.get(key) match {
        case Some(n) =>
            handleMandatoryText(key, n, textFieldKey, data, noRadioButtonSelectedMessageKey,
              blankValueMessageKey)
        case _ => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
      }
    }
    override def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
  }

  def handleMandatoryText(key: String, n: String, textFieldKey:String, data: Map[String, String], noRadioButtonSelectedMessageKey:String,
                          blankValueMessageKey:String ): Either[Seq[FormError], Boolean]  = {
    n.trim match {
      case p if p.length == 0 => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
      case "01" =>
          data.getOrElse(textFieldKey, "") match {
            case q if q.length == 0 => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
            case _ => Right(true)
          }
      case _ => Right(true)
    }
  }

  def mandatoryBooleanWithText(textFieldKey:String, noRadioButtonSelectedMessageKey: String,
                                blankValueMessageKey: String, notBlankValueMessageKey: String) = {
    Forms.of[Boolean](mandatoryRadioGroupWithTextFormatter(textFieldKey, noRadioButtonSelectedMessageKey, blankValueMessageKey))
  }
}
