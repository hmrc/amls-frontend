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

import org.scalatest.MustMatchers
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import cats.implicits._
import uk.gov.hmrc.play.http.HeaderCarrier
import audit.AddressConversions._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.AuditExtensions._

class AddressModifiedEventSpec extends PlaySpec with MustMatchers with OneAppPerSuite {

  implicit val hc = HeaderCarrier()
  implicit val request = FakeRequest("GET", "/test-path")

  "The AddressModifiedAuditEvent" must {
    "create the proper detail" when {
      "given a current address and an old address" in {
        val currentAddress = AuditAddress("Addr Line 1", "Addr Line 2", "Line 3".some, "Spain", "AA1 1AA".some)
        val oldAddress = AuditAddress("Old addr Line 1", "Old addr Line 2", "Old line 3".some, "France", "NE1 1ET".some)

        val expectedResult = hc.toAuditDetails() ++ Map(
          "addressLine1" -> "Addr Line 1",
          "addressLine2" -> "Addr Line 2",
          "addressLine3" -> "Line 3",
          "country" -> "Spain",
          "postCode" -> "AA1 1AA",
          "originalLine1" -> "Old addr Line 1",
          "originalLine2" -> "Old addr Line 2",
          "originalLine3" -> "Old line 3",
          "originalCountry" -> "France",
          "originalPostCode" -> "NE1 1ET"
        )

        val event: DataEvent = AddressModifiedEvent(currentAddress, oldAddress.some)

        event.detail mustBe expectedResult
        event.tags("path") mustBe "/test-path"
      }

      "given a current address and an old address without the optional fields" in {
        val currentAddress = AuditAddress("Addr Line 1", "Addr Line 2", None, "Spain", None)
        val oldAddress = AuditAddress("Old addr Line 1", "Old addr Line 2", None, "France", None)

        val expectedResult = hc.toAuditDetails() ++ Map(
          "addressLine1" -> "Addr Line 1",
          "addressLine2" -> "Addr Line 2",
          "country" -> "Spain",
          "originalLine1" -> "Old addr Line 1",
          "originalLine2" -> "Old addr Line 2",
          "originalCountry" -> "France"
        )

        val event: DataEvent = AddressModifiedEvent(currentAddress, oldAddress.some)

        event.detail mustBe expectedResult
      }
    }
  }
}
