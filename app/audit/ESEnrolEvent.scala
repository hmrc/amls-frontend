/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package audit

import models.enrolment.{EnrolmentKey, TaxEnrolment}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName

object ESEnrolEvent {
  def apply
  (enrolment: TaxEnrolment, response: HttpResponse, key: EnrolmentKey)
  (implicit
   hc: HeaderCarrier,
   reqW: Writes[TaxEnrolment]
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

object ESDeEnrolEvent {
  def apply
  (response: HttpResponse, enrolmentKey: String)
  (implicit
   hc: HeaderCarrier
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "OutboundCall",
      tags = hc.toAuditTags("DeEnrolment", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "enrolment" -> enrolmentKey,
        "response" -> response.body,
        "status" -> response.status.toString
      )
    )
}

object ESRemoveKnownFactsEvent {
  def apply
  (response: HttpResponse, enrolmentKey: String)
  (implicit
   hc: HeaderCarrier
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "OutboundCall",
      tags = hc.toAuditTags("RemoveKnownFacts", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "enrolment" -> enrolmentKey,
        "response" -> response.body,
        "status" -> response.status.toString
      )
    )
}


object ESEnrolFailureEvent {
  def apply
  (enrolment: TaxEnrolment, exception: Throwable, key: EnrolmentKey)
  (implicit
   hc: HeaderCarrier,
   reqW: Writes[TaxEnrolment]
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "OutboundCall",
      tags = hc.toAuditTags("Enrolment", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "enrolment" -> Json.toJson(enrolment).toString,
        "key" -> key.key,
        "exception" -> exception.getMessage
      )
    )
}
