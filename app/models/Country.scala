package models

import play.api.data.mapping._
import play.api.data.validation.ValidationError
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
        JsError(JsPath -> ValidationError("error.invalid"))
      }
    case _ =>
      JsError(JsPath -> ValidationError("error.invalid"))
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
    import play.api.data.mapping.json.Writes.string
    formWrites compose string
  }

  implicit val jsonR: Rule[JsValue, Country] = {
    import play.api.data.mapping.json.Rules.stringR
    stringR compose formRule
  }
}
