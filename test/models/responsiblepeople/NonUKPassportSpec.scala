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
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class NonUKPassportSpec extends PlaySpec {

  "NonUKPassport" when {

    "JSON" must {

      "Read the json and return the PassportType domain object successfully for the NoPassport" in {

        NonUKPassport.jsonReads.reads(NonUKPassport.jsonWrites.writes(NoPassport)) must
          be(JsSuccess(NoPassport, JsPath))
      }

      "Read the json and return NonUKPassport" in {
        val model = NonUKPassportYes("21321313213132132")
        NonUKPassport.jsonReads.reads(NonUKPassport.jsonWrites.writes(model)) must
          be(JsSuccess(model, JsPath \ "nonUKPassportNumber"))
      }

      "Read the json and return error if passport number is missing" in {
        val json = Json.obj("nonUKPassport" -> true)
        NonUKPassport.jsonReads.reads(json) must be(
          JsError((JsPath \ "nonUKPassportNumber") -> play.api.libs.json.JsonValidationError("error.path.missing"))
        )
      }
    }
  }
}
