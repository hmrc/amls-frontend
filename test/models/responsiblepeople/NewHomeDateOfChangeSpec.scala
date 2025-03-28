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
import play.api.libs.json._

import java.time.LocalDate

class NewHomeDateOfChangeSpec extends PlaySpec {
  "NewHomeDateOfChange" must {

    "Read and write successfully" in {
      NewHomeDateOfChange.format.reads(
        NewHomeDateOfChange.format.writes(NewHomeDateOfChange(Some(LocalDate.of(1990, 2, 24))))
      ) must be(JsSuccess(NewHomeDateOfChange(Some(LocalDate.of(1990, 2, 24))), JsPath))

    }

    "write successfully" in {
      NewHomeDateOfChange.format.writes(NewHomeDateOfChange(Some(LocalDate.of(1990, 2, 24)))) must be(
        Json.obj("dateOfChange" -> "1990-02-24")
      )
    }
  }
}
