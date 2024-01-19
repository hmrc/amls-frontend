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

package models.tcsp

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class OnlyOffTheShelfCompsSoldSpec extends PlaySpec with MustMatchers {

  "OnlyOffTheShelfCompsSold" must {

    "round trip through JSON" in {

      Json.toJson[OnlyOffTheShelfCompsSold](OnlyOffTheShelfCompsSoldYes)
        .as[OnlyOffTheShelfCompsSold] mustBe OnlyOffTheShelfCompsSoldYes
      Json.toJson[OnlyOffTheShelfCompsSold](OnlyOffTheShelfCompsSoldNo)
        .as[OnlyOffTheShelfCompsSold] mustBe OnlyOffTheShelfCompsSoldNo
    }
  }
}
