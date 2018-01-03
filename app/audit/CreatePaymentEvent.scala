/*
 * Copyright 2018 HM Revenue & Customs
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

import models.payments.{CreatePaymentRequest, CreatePaymentResponse}
import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.audit.AuditExtensions._

object CreatePaymentEvent {
  def apply(request: CreatePaymentRequest, response: CreatePaymentResponse)
           (implicit hc: HeaderCarrier,
            requestWrites: Writes[CreatePaymentRequest],
            responseWrites: Writes[CreatePaymentResponse]): ExtendedDataEvent = {
    ExtendedDataEvent(
      auditSource = AppName.appName,
      auditType = "createPaymentFailureEvent",
      tags = hc.toAuditTags("Create Payment", "n/a"),
      detail = Json.toJson(hc.toAuditDetails()).as[JsObject] ++ Json.obj(
        "request" -> request,
        "response" -> response
      )
    )
  }

}
