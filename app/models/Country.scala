package models

import jto.validation._
import jto.validation.ValidationError
import play.api.i18n.Messages
import play.api.libs.json._

case class Country(name: String, code: String) {
  override def toString: String = name
}

object Country {

  implicit val writes = Writes[Country] {
    case Country(_, c) => JsString(c)
  }

  implicit val reads = Reads[Country] {
    case JsString(code) =>
      countries collectFirst {
        case e @ Country(_, c) if c == code =>
          JsSuccess(e)
      } getOrElse {
        JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
      }
    case _ =>
      JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
  }

  implicit val formWrites: Write[Country, String] =
    Write { _.code }

  implicit val formRule: Rule[String, Country] =
    Rule {
      case "" => Failure(Seq(Path -> Seq(ValidationError("error.required.country"))))
      case code =>
        countries.collectFirst {
          case e @ Country(_, c) if c == code =>
            Success(e)
        } getOrElse {
          Failure(Seq(Path -> Seq(ValidationError("error.invalid.country"))))
        }
    }

  implicit val jsonW: Write[Country, JsValue] = {
    import jto.validation.playjson.Writes.string
    formWrites compose string
  }

  implicit val jsonR: Rule[JsValue, Country] = {
    import jto.validation.playjson.Rules.stringR
    stringR compose formRule
  }
}
