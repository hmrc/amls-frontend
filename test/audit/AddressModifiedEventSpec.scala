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

import audit.AddressConversions._
import cats.implicits._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.AmlsSpec

class AddressModifiedEventSpec extends AmlsSpec {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/test-path")
  implicit override val headerCarrier: HeaderCarrier        = HeaderCarrier()

  "The AddressModifiedAuditEvent" must {
    "create the proper detail" when {
      "given a current address and an old address" in {
        val currentAddress = AuditAddress("Addr Line 1", "Addr Line 2".some, "Line 3".some, "Spain", "AA1 1AA".some)
        val oldAddress     =
          AuditAddress("Old addr Line 1", "Old addr Line 2".some, "Old line 3".some, "France", "NE1 1ET".some)

        val expectedResult = headerCarrier.toAuditDetails() ++ Map(
          "addressLine1"     -> "Addr Line 1",
          "addressLine2"     -> "Addr Line 2",
          "addressLine3"     -> "Line 3",
          "country"          -> "Spain",
          "postCode"         -> "AA1 1AA",
          "originalLine1"    -> "Old addr Line 1",
          "originalLine2"    -> "Old addr Line 2",
          "originalLine3"    -> "Old line 3",
          "originalCountry"  -> "France",
          "originalPostCode" -> "NE1 1ET"
        )

        val event: DataEvent = AddressModifiedEvent(currentAddress, oldAddress.some)

        event.detail mustBe expectedResult
        event.tags("path") mustBe "/test-path"
      }

      "given a current address and an old address without the optional fields" in {
        val currentAddress = AuditAddress("Addr Line 1", None, None, "Spain", None)
        val oldAddress     = AuditAddress("Old addr Line 1", None, None, "France", None)

        val expectedResult = headerCarrier.toAuditDetails() ++ Map(
          "addressLine1"    -> "Addr Line 1",
          "country"         -> "Spain",
          "originalLine1"   -> "Old addr Line 1",
          "originalCountry" -> "France"
        )

        val event: DataEvent = AddressModifiedEvent(currentAddress, oldAddress.some)

        event.detail mustBe expectedResult
      }
    }
  }
}
