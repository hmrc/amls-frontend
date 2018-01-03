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

import models.governmentgateway.EnrolmentRequest
import models.payments
import models.payments.PaymentStatuses._
import models.payments.{PaymentStatus, PaymentStatusResult, PaymentStatuses}
import play.api.libs.json.Writes
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.http.HeaderCarrier

object PaymentConfirmationEvent {
  def apply(amlsRef: String, payRef: String, paymentStatus: PaymentStatus)(implicit
                                                                           hc: HeaderCarrier,
                                                                           reqW: Writes[EnrolmentRequest]
  ): DataEvent =
    DataEvent(
      auditSource = AppName.appName,
      auditType = "paymentConfirm",
      tags = hc.toAuditTags("Payment Confirmation", "N/A"),
      detail = hc.toAuditDetails() ++ Map(
        "amlsReferenceNumber" -> amlsRef,
        "paymentReference" -> payRef,
        "paymentStatus" -> msgFromPaymentStatus(paymentStatus)
      )
    )

  val msgFromPaymentStatus = Map[PaymentStatus, String](
    PaymentStatuses.Failed -> "failed",
    PaymentStatuses.Cancelled -> "cancelled",
    PaymentStatuses.Successful -> "successful",
    PaymentStatuses.Sent -> "sent",
    PaymentStatuses.Created -> "created"
  )
}
