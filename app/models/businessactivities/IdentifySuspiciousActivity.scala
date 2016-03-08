package models.businessactivities

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class IdentifySuspiciousActivity(hasWrittenGuidance: Boolean)

object IdentifySuspiciousActivity {

  implicit val formats = Json.format[IdentifySuspiciousActivity]

  implicit val formRule: Rule[UrlFormEncoded, IdentifySuspiciousActivity] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "hasWrittenGuidance").read[Boolean] fmap (IdentifySuspiciousActivity.apply)
    }

  implicit val formWrites: Write[IdentifySuspiciousActivity, UrlFormEncoded] =
    Write {
      case IdentifySuspiciousActivity(b) =>
        Map("hasWrittenGuidance" -> Seq(b.toString))
    }
}