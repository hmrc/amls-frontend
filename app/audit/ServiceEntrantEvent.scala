/*
 * Copyright 2017 HM Revenue & Customs
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

import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.audit.AuditExtensions._
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier

object ServiceEntrantEvent {

  def apply(companyName: String, utr: String, safeId: String)(implicit hc: HeaderCarrier, request: Request[_]) = {
    val data = Json.toJson(hc.toAuditDetails()).as[JsObject] ++ Json.obj(
      "companyName" -> companyName,
      "utr" -> utr,
      "safeId"-> safeId
    )

    ExtendedDataEvent(
      auditSource = AppName.appName,
      auditType = "userEnteredService",
      tags = hc.toAuditTags("userEnteredService", request.path),
      detail = data
    )
  }
}
