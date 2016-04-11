package audit

import models.governmentgateway.EnrolmentRequest
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.AuditExtensions._

object EnrolEvent {
  def apply
  (request: EnrolmentRequest, response: HttpResponse)
  (implicit
   hc: HeaderCarrier,
   reqW: Writes[EnrolmentRequest]
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "OutboundCall",
      tags = hc.toAuditTags("Enrolment", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "request" -> Json.toJson(request).toString,
        "response" -> response.body
      )
    )
}
