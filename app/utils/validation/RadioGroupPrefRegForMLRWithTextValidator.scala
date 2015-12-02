package utils.validation

import play.api.data.{FormError, Forms}
import play.api.data.format.Formatter

object RadioGroupPrefRegForMLRWithTextValidator extends FormValidator {
  private def mandatoryRadioGroupWithTextFormatter(textFieldKey:String, textField2Key:String ,noRadioButtonSelectedMessageKey:String,
                                                   blankValueMessageKey:String, notBlankValueMessageKey:String) = new Formatter[(Boolean, Boolean)] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], (Boolean, Boolean)] = {
      data.get(key) match {
        case Some(n) => handleMandatoryText(key, n, textFieldKey, textField2Key,  data, noRadioButtonSelectedMessageKey,
              blankValueMessageKey, notBlankValueMessageKey)
        case _ => Left(Seq(FormError(key, noRadioButtonSelectedMessageKey)))
      }
    }
    override def unbind(key: String, value: (Boolean, Boolean)): Map[String, String] = {
      val gg = value match {
        case (true, false) => "01"
        case (false, true) => "02"
        case (false, false) => "03"
        case _ => throw new RuntimeException("Unknown tuple values")
      }
      Map(key -> gg )
    }
  }

  private def handleMandatoryText(key: String, n: String, textFieldKey:String, textField2Key:String, data: Map[String, String],
                                  noRadioButtonSelectedMessageKey:String, blankValueMessageKey:String,
                                  notBlankValueMessageKey:String ): Either[Seq[FormError], (Boolean, Boolean)]  = {
    n.trim match {
      case "01" =>handleYesOption01(textFieldKey, textField2Key, data, blankValueMessageKey, notBlankValueMessageKey)
      case "02" =>  getFieldError0102(textFieldKey, data, notBlankValueMessageKey) match {
        case None => Right(Tuple2(false, true))
        case Some(error) => Left(Seq(error))
      }
      case "03" => handleOption03(textFieldKey, textField2Key,data, notBlankValueMessageKey)

      case _ => Left(Seq(FormError("", noRadioButtonSelectedMessageKey)))
    }
  }

  private def handleYesOption01(textFieldKey:String, textField2Key:String, data: Map[String, String], blankValueMessageKey:String,
                   notBlankValueMessageKey:String ) :  Either[Seq[FormError], (Boolean, Boolean)] = {
    getFieldError0102(textField2Key, data, notBlankValueMessageKey) match {
      case None =>
        data.getOrElse (textFieldKey, "") match {
          case q if q.length == 0 => Left (Seq (FormError (textFieldKey, blankValueMessageKey) ) )
          case _ => Right (Tuple2 (true, false) )
        }
      case Some(error) => Left(Seq(error))
    }
  }
  private def getFieldError0102(textFieldKey:String, data: Map[String, String], notBlankValueMessageKey:String): Option[FormError] = {
    val test1 =  data.getOrElse(textFieldKey, "")
    test1.length>0 match {
      case true => Some(FormError (textFieldKey, notBlankValueMessageKey) )
      case _ => None
    }
  }

  private def handleOption03(textFieldKey:String, textField2Key:String, data: Map[String, String],
                             notBlankValueMessageKey:String): Either[Seq[FormError], (Boolean, Boolean)] = {
    val test1 =  data.getOrElse(textFieldKey, "")
    val test2 =  data.getOrElse(textField2Key, "")
    (test1.length>0,test2.length>0) match {
      case (true, false) => Left (Seq (FormError (textFieldKey, notBlankValueMessageKey) ) )
      case (false, true) => Left (Seq (FormError (textField2Key, notBlankValueMessageKey) ) )
      case (true, true) => Left (Seq (FormError (textFieldKey, notBlankValueMessageKey), FormError (textField2Key, notBlankValueMessageKey) ) )
      case _ => Right(Tuple2(false, false))
    }
   }

  def mandatoryBooleanWithText(textFieldKey:String, textField2Key:String, noRadioButtonSelectedMessageKey: String,
                                blankValueMessageKey: String, notBlankValueMessageKey: String) = {
    Forms.of[(Boolean, Boolean)](mandatoryRadioGroupWithTextFormatter(textFieldKey, textField2Key, noRadioButtonSelectedMessageKey,
      blankValueMessageKey, notBlankValueMessageKey))
  }
}
