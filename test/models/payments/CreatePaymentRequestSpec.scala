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

package models.payments

import config.ApplicationConfig
import models.ReturnLocation
import org.mockito.Mockito.when
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest

class CreatePaymentRequestSpec extends WordSpec with MustMatchers with MockitoSugar {

  implicit val request = FakeRequest("GET", "http://localhost:9222")

  "The CreatePaymentRequest model" must {
    "round-trip through JSON serialization correctly" in {

      val expectedJson =
        """
          | {
          |   "taxType":"other",
          |   "reference":"XA2345678901232",
          |   "description":"A description",
          |   "amountInPence":100,
          |   "returnUrl":"http://localhost:9222/confirmation"
          | }
        """.stripMargin

      //noinspection ScalaStyle

      val applicationConfig = mock[ApplicationConfig]

      when(applicationConfig.frontendBaseUrl).thenReturn("http://localhost:9222/")

      val model = CreatePaymentRequest("other", "XA2345678901232", "A description", 100, ReturnLocation("confirmation")(applicationConfig))

      Json.toJson(model) mustBe Json.parse(expectedJson)
    }
  }

}
