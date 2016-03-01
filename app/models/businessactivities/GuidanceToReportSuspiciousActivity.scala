package models.businessactivities

import play.api.data.mapping.forms._
import play.api.data.mapping.{From, Rule, Write}
import play.api.libs.json.Json

case class GuidanceToReportSuspiciousActivity(hasWrittenGuidance: Boolean)

object  {

  implicit val formats = Json.format[GuidanceToReportSuspiciousActivity]

  implicit val formRule: Rule[UrlFormEncoded, GuidanceToReportSuspiciousActivity] =
    From[UrlFormEncoded] { __ =>
      import play.api.data.mapping.forms.Rules._
      (__ \ "hasWrittenGuidance").read[Boolean] fmap (GuidanceToReportSuspiciousActivity.apply)
    }

  implicit val formWrites: Write[GuidanceToReportSuspiciousActivity, UrlFormEncoded] =
    Write {
      case GuidanceToReportSuspiciousActivity(b) =>
        Map("hasWrittenGuidance" -> Seq(b.toString))
    }
}