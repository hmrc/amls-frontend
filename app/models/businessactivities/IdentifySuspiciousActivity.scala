package models.businessactivities

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json

case class IdentifySuspiciousActivity(hasWrittenGuidance: Boolean)

object IdentifySuspiciousActivity {

  implicit val formats = Json.format[IdentifySuspiciousActivity]

  implicit val formRule: Rule[UrlFormEncoded, IdentifySuspiciousActivity] =
    From[UrlFormEncoded] { __ =>
      import jto.validation.forms.Rules._
      import utils.MappingUtils.Implicits._
      (__ \ "hasWrittenGuidance").read[Boolean].withMessage("error.required.ba.suspicious.activity") fmap (IdentifySuspiciousActivity.apply)
    }

  implicit val formWrites: Write[IdentifySuspiciousActivity, UrlFormEncoded] =
    Write {
      case IdentifySuspiciousActivity(b) =>
        Map("hasWrittenGuidance" -> Seq(b.toString))
    }
}
