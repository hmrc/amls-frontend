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

package models.moneyservicebusiness

import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class IdentifyLinkedTransactionsSpec extends PlaySpec {

  "IdentifyLinkedTransactions" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("linkedTxn" -> Seq("true"))

        IdentifyLinkedTransactions.formRule.validate(map) must be(Valid(IdentifyLinkedTransactions(true)))
      }

      "Successfully read form data for option no" in {

        val map = Map("linkedTxn" -> Seq("false"))

        IdentifyLinkedTransactions.formRule.validate(map) must be(Valid(IdentifyLinkedTransactions(false)))
      }

      "fail validation on missing field" in {

        IdentifyLinkedTransactions.formRule.validate(Map.empty) must be(Invalid(
          Seq( Path \ "linkedTxn" -> Seq(ValidationError("error.required.msb.linked.txn")))))
      }

      "successfully write form data" in {

        IdentifyLinkedTransactions.formWrites.writes(IdentifyLinkedTransactions(false)) must be(Map("linkedTxn" -> Seq("false")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        IdentifyLinkedTransactions.format.reads(IdentifyLinkedTransactions.format.writes(
          IdentifyLinkedTransactions(false))) must be(JsSuccess(IdentifyLinkedTransactions(false), JsPath \ "linkedTxn"))

      }
    }
  }
}
