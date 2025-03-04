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
import play.api.libs.json.{JsPath, JsSuccess, Json}

import java.time.LocalDate

class DateOfBirthSpec extends PlaySpec {

  val validYear  = 1990
  val validDay   = 24
  val validMonth = 2

  "DateOfBirth Json" must {

    "Read and write successfully" in {

      DateOfBirth.format.reads(
        DateOfBirth.format.writes(DateOfBirth(LocalDate.of(validYear, validMonth, validDay)))
      ) must be(
        JsSuccess(DateOfBirth(LocalDate.of(validYear, validMonth, validDay)), JsPath)
      )

    }

    "write successfully" in {
      DateOfBirth.format.writes(DateOfBirth(LocalDate.of(validYear, validMonth, validDay))) must be(
        Json.obj("dateOfBirth" -> "1990-02-24")
      )
    }
  }

}
