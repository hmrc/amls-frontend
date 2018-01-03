/*
 * Copyright 2018 HM Revenue & Customs
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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}


class IsResidentialSpec extends PlaySpec {
  // scalastyle:off

  "Form validation" must {
    "pass validation" when {
      "given a valid answer" in {

        val model = Map(
          "isResidential" -> Seq("true")
        )

        IsResidential.formRule.validate(model) must be(Valid(IsResidential(true)))
      }
    }

    "fail validation" when {
      "given missing data represented by an empty string" in {

        val model = Map(
          "isResidential" -> Seq("")
        )
        IsResidential.formRule.validate(model) must be(Invalid(Seq(
          Path \ "isResidential" -> Seq(ValidationError("tradingpremises.yourtradingpremises.isresidential.required"))
        )))
      }

      "given missing data represented by an empty Map" in {

        IsResidential.formRule.validate(Map.empty) must be(Invalid(Seq(
          Path \ "isResidential" -> Seq(ValidationError("tradingpremises.yourtradingpremises.isresidential.required"))
        )))
      }
    }

    "successfully write the model" in {

      IsResidential.formWrites.writes(IsResidential(true)) mustBe Map(
        "isResidential" -> Seq("true")
      )
    }
  }

  "Json validation" must {

    "Read and write successfully" in {

      IsResidential.format.reads(IsResidential.format.writes(IsResidential(true))) must be(
        JsSuccess(IsResidential(true), JsPath \ "isResidential"))
    }

    "write successfully" in {
      IsResidential.format.writes(IsResidential(true)) must be(Json.obj("isResidential" -> true))
    }
  }
}
