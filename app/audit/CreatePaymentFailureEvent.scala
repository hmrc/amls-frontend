/*
 * Copyright 2024 HM Revenue & Customs
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

import models.payments.CreatePaymentRequest
import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.AuditHelper

object CreatePaymentFailureEvent {
  def apply(paymentRef: String, status: Int, message: String, request: CreatePaymentRequest)(implicit
    hc: HeaderCarrier,
    requestWrites: Writes[CreatePaymentRequest]
  ): ExtendedDataEvent =
    ExtendedDataEvent(
      auditSource = AuditHelper.appName,
      auditType = "createPaymentFailureEvent",
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags("Create Payment", "n/a"),
      detail = Json.toJson(AuditExtensions.auditHeaderCarrier(hc).toAuditDetails()).as[JsObject] ++ Json.obj(
        "paymentRef" -> paymentRef,
        "status"     -> status,
        "message"    -> message,
        "request"    -> request
      )
    )

}
