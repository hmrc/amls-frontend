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
    country => JsString(country.code)
  }

  implicit val reads = Reads[Country] {
    case JsString(code) =>
      countries collectFirst[JsResult[Country]] {
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
      case "" => Failure(Seq(Path -> Seq(ValidationError(Messages("error.required.country")))))
      case code =>
        countries.collectFirst {
          case e @ Country(_, c) if c == code =>
            Success(e)
        } getOrElse {
          Failure(Seq(Path -> Seq(ValidationError(Messages("error.invalid.country")))))
        }
    }
}
