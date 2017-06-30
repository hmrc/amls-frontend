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

package models.changeofficer

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import models.changeofficer._

class ChangeOfficerSpec extends PlaySpec with MustMatchers {

  "The ChangeOfficer model" must {
    "successfully convert json to the model" in {
      val model = ChangeOfficer(RoleInBusiness(Set(Director, Other("Another role"))))

      Json.toJson(model).as[ChangeOfficer] mustBe model
    }

  }

}
