package audit

import models.SatisfactionSurvey
import models.governmentgateway.EnrolmentRequest
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.{HeaderCarrier}
import uk.gov.hmrc.play.audit.AuditExtensions._

object SurveyEvent {
  def apply
  (survey: SatisfactionSurvey)
  (implicit
   hc: HeaderCarrier,
   reqW: Writes[EnrolmentRequest]
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "SurveyCompleted",
      tags = hc.toAuditTags("SatisfactionSurvey", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "survey" -> Json.toJson(survey).toString
      )
    )
}
