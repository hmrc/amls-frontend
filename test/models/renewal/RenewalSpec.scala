/*
 * Copyright 2019 HM Revenue & Customs
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

package models.renewal

import models.Country
import play.api.libs.json.{JsSuccess, Json}
import utils.AmlsSpec

class RenewalSpec extends AmlsSpec {

  "The Renewal model" must {

    "serialize to and from JSON" in {

      val model = Renewal()

      Json.fromJson[Renewal](Json.toJson(model)) mustBe JsSuccess(model)

    }

  }
}