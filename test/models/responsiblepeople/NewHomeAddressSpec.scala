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

package models.responsiblepeople

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class NewHomeAddressSpec extends PlaySpec {

  "NewHomeAddress" must {

    "Round trip through Json" in {
      val model = NewHomeAddress(
        PersonAddressUK(
          "some address1",
          Some("some address2"),
          Some("Default Line 3"),
          Some("Default Line 4"),
          "AA1 1AA"
        )
      )

      Json.toJson(model).as[NewHomeAddress] mustBe model
    }
  }
}
