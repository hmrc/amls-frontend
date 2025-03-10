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

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class TrainingSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[Training](Json.obj("training" -> false)) must
        be(JsSuccess(TrainingNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("training" -> true, "information" -> "0123456789")

      Json.fromJson[Training](json) must
        be(JsSuccess(TrainingYes("0123456789"), JsPath \ "information"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("training" -> true)

      Json.fromJson[Training](json) must
        be(JsError((JsPath \ "information") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(TrainingNo.asInstanceOf[Training]) must be(Json.obj("training" -> false))

      Json.toJson(TrainingYes("0123456789").asInstanceOf[Training]) must
        be(
          Json.obj(
            "training"    -> true,
            "information" -> "0123456789"
          )
        )
    }
  }

}
