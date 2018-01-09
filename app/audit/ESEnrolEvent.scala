package audit

import models.enrolment.{EnrolmentKey, EnrolmentStoreEnrolment}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName

object ESEnrolEvent {
  def apply
  (enrolment: EnrolmentStoreEnrolment, response: HttpResponse, key: EnrolmentKey)
  (implicit
   hc: HeaderCarrier,
   reqW: Writes[EnrolmentStoreEnrolment],
   keyW: Writes[EnrolmentKey]
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "OutboundCall",
      tags = hc.toAuditTags("Enrolment", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "enrolment" -> Json.toJson(enrolment).toString,
        "key" -> key.key,
        "response" -> response.body,
        "status" -> response.status.toString
      )
    )
}
