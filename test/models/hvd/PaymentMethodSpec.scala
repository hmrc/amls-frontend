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
import play.api.libs.json.{JsPath, Json, JsSuccess}

class PaymentMethodSpec extends PlaySpec {

  "PaymentMethod" must {

    "roundtrip through form" in {
      val data = PaymentMethods(courier = true, direct = true, other = Some("foo"))
      PaymentMethods.formR.validate(PaymentMethods.formW.writes(data)) mustEqual Valid(data)
    }

    "roundtrip through json" in {
      val data = PaymentMethods(courier = true, direct = true, other = Some("foo"))
      val js = Json.toJson(data)
      js.as[PaymentMethods] mustEqual data
    }

    "fail to validate when no payment method is selected" in {
      val data = Map.empty[String, Seq[String]]
      PaymentMethods.formR.validate(data) mustEqual Invalid(Seq(Path -> Seq(ValidationError("error.required.hvd.choose.option"))))
    }

    "fail to validate when other is selected without details" in {
      val data = Map(
        "other" -> Seq("true"),
        "details" -> Seq("")
      )
      PaymentMethods.formR.validate(data) mustEqual Invalid(Seq((Path \ "details") -> Seq(ValidationError("error.required.hvd.describe"))))
    }

    "fail to validate when invalid characters are specified in 'other details'" in {
      val data = Map(
        "other" -> Seq("true"),
        "details" -> Seq("¡<>{}")
      )

      PaymentMethods.formR.validate(data) must be(
        Invalid(Seq((Path \ "details") -> Seq(ValidationError("err.text.validation"))))
      )
    }
  }
}
