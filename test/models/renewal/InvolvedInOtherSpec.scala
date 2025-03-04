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

package models.renewal

import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class InvolvedInOtherSpec extends PlaySpec with MockitoSugar {

  "JSON validation" must {
    "successfully validate given an enum value" in {

      Json.fromJson[InvolvedInOther](Json.obj("involvedInOther" -> false)) must
        be(JsSuccess(InvolvedInOtherNo, JsPath))
    }

    "successfully validate given an `Yes` value" in {

      val json = Json.obj("involvedInOther" -> true, "details" -> "test")

      Json.fromJson[InvolvedInOther](json) must
        be(JsSuccess(InvolvedInOtherYes("test"), JsPath \ "details"))
    }

    "fail to validate when given an empty `Yes` value" in {

      val json = Json.obj("involvedInOther" -> true)

      Json.fromJson[InvolvedInOther](json) must
        be(JsError((JsPath \ "details") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "write the correct value" in {

      Json.toJson(InvolvedInOtherNo.asInstanceOf[InvolvedInOther]) must
        be(Json.obj("involvedInOther" -> false))

      Json.toJson(InvolvedInOtherYes("test").asInstanceOf[InvolvedInOther]) must
        be(
          Json.obj(
            "involvedInOther" -> true,
            "details"         -> "test"
          )
        )
    }
  }

}
