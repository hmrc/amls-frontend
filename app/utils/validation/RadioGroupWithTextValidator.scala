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
                                  noRadioButtonSelectedMessageKey:String, blankValueMessageKey:String,
                                  notBlankValueMessageKey:String ): Either[Seq[FormError], (Boolean, Boolean)]  = {
    n.trim match {
      case "01" =>
          data.getOrElse(textFieldKey, "") match {
            case q if q.length == 0 => Left(Seq(FormError(textFieldKey, blankValueMessageKey)))
            case _ => Right(Tuple2(true, false))
          }
      case "02" => Right(Tuple2(false, true))
      case "03" => handleOption03(textFieldKey, textField2Key,data, notBlankValueMessageKey)

      case _ => Left(Seq(FormError("", noRadioButtonSelectedMessageKey)))
    }
  }

  private def handleOption03(textFieldKey:String, textField2Key:String, data: Map[String, String],
                             notBlankValueMessageKey:String): Either[Seq[FormError], (Boolean, Boolean)] = {
    val test1 =  data.get(textFieldKey)
    val test2 =  data.get(textField2Key)
    (test1,test2) match {
      case (Some(_), None) => Left (Seq (FormError (textFieldKey, notBlankValueMessageKey) ) )
      case (None, Some(_)) => Left (Seq (FormError (textField2Key, notBlankValueMessageKey) ) )
      case (Some(_), Some(_)) => Left (Seq (FormError (textFieldKey, notBlankValueMessageKey), FormError (textField2Key, notBlankValueMessageKey) ) )
      case _ => Right(Tuple2(false, false))
    }
   }


  def mandatoryBooleanWithText(textFieldKey:String, textField2Key:String, noRadioButtonSelectedMessageKey: String,
                                blankValueMessageKey: String, notBlankValueMessageKey: String) = {
    Forms.of[(Boolean, Boolean)](mandatoryRadioGroupWithTextFormatter(textFieldKey, textField2Key, noRadioButtonSelectedMessageKey,
      blankValueMessageKey, notBlankValueMessageKey))
  }
}
