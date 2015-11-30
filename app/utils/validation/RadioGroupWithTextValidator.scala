package utils.validation

import play.api.data.{FormError, Forms}
import play.api.data.format.Formatter

object RadioGroupWithTextValidator extends FormValidator {
  private def mandatoryRadioGroupWithTextFormatter(textFieldKey:String, textField2Key:String ,noRadioButtonSelectedMessageKey:String,
                                                   blankValueMessageKey:String, notBlankValueMessageKey:String) = new Formatter[(Boolean, Boolean)] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], (Boolean, Boolean)] = {
      data.get(key) match {
        case Some(n) => handleMandatoryText(key, n, textFieldKey, textField2Key,  data, noRadioButtonSelectedMessageKey,
              blankValueMessageKey, notBlankValueMessageKey)
        case _ => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
      }
    }
    override def unbind(key: String, value: (Boolean, Boolean)): Map[String, String] = Map(key -> value.toString)
  }

  private def handleMandatoryText(key: String, n: String, textFieldKey:String, textField2Key:String, data: Map[String, String],
                                  noRadioButtonSelectedMessageKey:String, blankValueMessageKey:String, notBlankValueMessageKey:String ): Either[Seq[FormError], (Boolean, Boolean)]  = {
    n.trim match {
      case p if p.length == 0 => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
      case "01" =>
          data.getOrElse(textFieldKey, "") match {
            case q if q.length == 0 => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
            case _ => Right(Tuple2(true, false))
          }
      case "02" => Right(Tuple2(false, true))
      case "03" => handleOption03(textFieldKey, textField2Key, notBlankValueMessageKey)

      case _ => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
    }
  }
  private def handleOption03(textFieldKey:String, textField2Key:String, notBlankValueMessageKey:String): Either[Seq[FormError], (Boolean, Boolean)] = {
    
    Right(Tuple2(false, false))
  }

  def mandatoryBooleanWithText(textFieldKey:String, textField2Key:String, noRadioButtonSelectedMessageKey: String,
                                blankValueMessageKey: String, notBlankValueMessageKey: String) = {
    Forms.of[(Boolean, Boolean)](mandatoryRadioGroupWithTextFormatter(textFieldKey, textField2Key, noRadioButtonSelectedMessageKey,
      blankValueMessageKey, notBlankValueMessageKey))
  }
}
