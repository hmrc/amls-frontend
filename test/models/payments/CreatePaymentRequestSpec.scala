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

package models.payments

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class CreatePaymentRequestSpec extends PlaySpec with MustMatchers {

  "The CreatePaymentRequest model" must {
    "round-trip through JSON serialization correctly" in {
      //noinspection ScalaStyle
      val model = CreatePaymentRequest("other", "Some Reference", "A description", 100, "http://return.url")

      Json.toJson(model).as[CreatePaymentRequest] mustBe model
    }
  }

}
