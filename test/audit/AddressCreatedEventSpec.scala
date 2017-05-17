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

import models.responsiblepeople.{PersonAddressNonUK, PersonAddressUK}
import org.scalatest.MustMatchers
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import cats.implicits._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import audit.AddressConversions._
import models.Country
import models.aboutthebusiness.RegisteredOfficeUK
import models.tradingpremises.{Address => TradingPremisesAddress}

class AddressCreatedEventSpec extends PlaySpec with MustMatchers with OneAppPerSuite {

  implicit val hc = HeaderCarrier()

  "The AddressCreatedAuditEvent" must {
    "create the proper detail" when {
      "given an address of a responsible person in the UK" in {

        val address = PersonAddressUK("Line 1", "Line 2", "Line 3".some, None, "postcode")
        val event = AddressCreatedEvent(address)
        val expected = hc.toAuditDetails() ++ Map(
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "addressLine3" -> "Line 3",
          "country" -> "GB",
          "postCode" -> "postcode"
        )

        event.detail mustBe expected
      }

      "given an address of a responsible person outside the UK" in {
        val address = PersonAddressNonUK("Line 1", "Line 2", "Line 3".some, None, Country("Norway", "NW"))
        val event = AddressCreatedEvent(address)
        val expected = hc.toAuditDetails() ++ Map(
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "addressLine3" -> "Line 3",
          "country" -> "Norway"
        )

        event.detail mustBe expected
      }

      "given the address of a trading premises" in {
        val address = TradingPremisesAddress("TP Line 1", "TP Line 2", "TP Line 3".some, None, "a post code")
        val event = AddressCreatedEvent(address)
        val expected = hc.toAuditDetails() ++ Map(
          "addressLine1" -> "TP Line 1",
          "addressLine2" -> "TP Line 2",
          "addressLine3" -> "TP Line 3",
          "country" -> "GB",
          "postCode" -> "a post code"
        )

        event.detail mustBe expected
      }

      "given the address of a registered office in the UK" in {
        val address = RegisteredOfficeUK("RO Line 1", "RO Line 2", "RO Line 3".some, None, "a post code")
        val event = AddressCreatedEvent(address)
        val expected = hc.toAuditDetails() ++ Map(
          "addressLine1" -> "RO Line 1",
          "addressLine2" -> "RO Line 2",
          "addressLine3" -> "RO Line 3",
          "country" -> "GB",
          "postCode" -> "a post code"
        )

        event.detail mustBe expected
      }
    }
  }
}
