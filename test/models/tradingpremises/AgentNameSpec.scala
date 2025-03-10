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

package models.tradingpremises

import models.DateOfChange
import play.api.libs.json.JsSuccess
import utils.AmlsSpec

import java.time.LocalDate

class AgentNameSpec extends AmlsSpec {

  "Json Validation" must {
    "Successfully read/write Json data" in {
      AgentName.format.reads(
        AgentName.format.writes(AgentName("test", Some(DateOfChange(LocalDate.of(2017, 1, 1)))))
      ) must be(JsSuccess(AgentName("test", Some(DateOfChange(LocalDate.of(2017, 1, 1))))))
    }

    "Succesfully read/write Json data with agent dob" in {

      AgentName.format.reads(
        AgentName.format.writes(
          AgentName("test", Some(DateOfChange(LocalDate.of(2017, 1, 1))), Some(LocalDate.of(2015, 10, 10)))
        )
      ) must be(
        JsSuccess(AgentName("test", Some(DateOfChange(LocalDate.of(2017, 1, 1))), Some(LocalDate.of(2015, 10, 10))))
      )

    }

  }

}
