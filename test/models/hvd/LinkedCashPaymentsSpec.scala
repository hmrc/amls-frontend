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
import jto.validation.{Path, Invalid, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class LinkedCashPaymentsSpec extends PlaySpec {

  "LinkedCashPayments" should {

    "Form Validation:" must {

      "successfully validate given an valid 'yes' option" in {
        val map = Map {
          "linkedCashPayments" -> Seq("true")
        }

        LinkedCashPayments.formRule.validate(map) must be(Valid(LinkedCashPayments(true)))
      }

      "successfully validate given an valid 'no' option" in {
        val map = Map {
          "linkedCashPayments" -> Seq("false")
        }

        LinkedCashPayments.formRule.validate(map) must be(Valid(LinkedCashPayments(false)))
      }

      "fail validation when no option selected" in {
        LinkedCashPayments.formRule.validate(Map.empty) must be(Invalid(
          Seq(Path \ "linkedCashPayments" -> Seq(ValidationError("error.required.hvd.linked.cash.payment")))))

      }

      "successfully write form data" in {

        LinkedCashPayments.formWrites.writes(LinkedCashPayments(true)) must be(Map("linkedCashPayments" -> Seq("true")))

      }
    }

    "Json Validation" must {

      "successfully read and write json data" in {

        LinkedCashPayments.format.reads(LinkedCashPayments.format.writes(LinkedCashPayments(true))) must be(JsSuccess(LinkedCashPayments(true),
          JsPath \ "linkedCashPayments"))

      }
    }
  }
}
