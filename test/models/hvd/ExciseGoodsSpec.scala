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

package models.hvd

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class ExciseGoodsSpec extends PlaySpec {

  "ExciseGoods" should {

    "Form Validation:" must {

      "successfully validate given an valid 'yes' option" in {
        val map = Map {
          "exciseGoods" -> Seq("true")
        }

        ExciseGoods.formRule.validate(map) must be(Valid(ExciseGoods(true)))
      }

      "successfully validate given an valid 'no' option" in {
        val map = Map {
          "exciseGoods" -> Seq("false")
        }

        ExciseGoods.formRule.validate(map) must be(Valid(ExciseGoods(false)))
      }

      "fail when neither option has been selected" in {

        ExciseGoods.formRule.validate(Map.empty) must be(Invalid(
          Seq(Path \ "exciseGoods" -> Seq(ValidationError("error.required.hvd.excise.goods")))))

      }

      "successfully write form data" in {

        ExciseGoods.formWrites.writes(ExciseGoods(true)) must be(Map("exciseGoods" -> Seq("true")))

      }
    }

    "Json Validation" must {

      "successfully read and write json data" in {

        ExciseGoods.format.reads(ExciseGoods.format.writes(ExciseGoods(true))) must be(JsSuccess(ExciseGoods(true),
          JsPath \ "exciseGoods"))

      }
    }
  }
}
