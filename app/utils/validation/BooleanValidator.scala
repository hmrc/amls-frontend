package utils.validation

import play.api.data.format.Formatter
import play.api.data.{FormError, Forms}

object BooleanValidator extends BooleanValidator

class BooleanValidator extends FormValidator {

  private def mandatoryBooleanFormatter(blankValueMessageKey: String) = new Formatter[Boolean] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] = {
      data.get(key) match {
        case Some(value) => value match {
          case "true" => Right(true)
          case "false" => Right(false)
          case _ => Left(Seq(FormError(key, blankValueMessageKey, Nil)))
        }
        case _ => Left(Seq(FormError(key, blankValueMessageKey, Nil)))
      }
    }

    override def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
  }


  def mandatoryBoolean(blankValueMessageKey: String) =
    Forms.of[Boolean](mandatoryBooleanFormatter(blankValueMessageKey))

}
