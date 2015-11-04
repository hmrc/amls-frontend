package utils.validation

import play.api.data.Forms
import play.api.data.format.Formatter
import play.api.data.FormError
import config.AmlsPropertiesReader.getProperty

object NinoValidator extends NinoValidator

class NinoValidator extends FormValidator {

  private def mandatoryNinoFormatter(blankValueMessageKey: String, invalidLengthMessageKey: String,
                                     invalidValueMessageKey: String) = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(n) =>
          val s = n.trim.replaceAll(" ", "")
          s match{
            case p if p.length==0 => Left(Seq(FormError(key, blankValueMessageKey)))
            case num => {
              if (num.length > getProperty("validationMaxLengthNINO").toInt) {
                Left(Seq(FormError(key, invalidLengthMessageKey)))
              } else {
                ninoRegex.findFirstIn(num) match {
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

  def mandatoryNino( blankValueMessageKey: String,
                     invalidLengthMessageKey: String,
                     invalidValueMessageKey: String) =
    Forms.of[String](mandatoryNinoFormatter(blankValueMessageKey,
      invalidLengthMessageKey, invalidValueMessageKey))
}
