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

package models.renewal

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess}

class SendMoneyToOtherCountrySpec extends PlaySpec {

  "SendMoneyToOtherCountry" should {

    "Form Validation" must {

      "Successfully read form data for option yes" in {

        val map = Map("money" -> Seq("true"))

        SendMoneyToOtherCountry.formRule.validate(map) must be(Valid(SendMoneyToOtherCountry(true)))
      }

      "Successfully read form data for option no" in {

        val map = Map("money" -> Seq("false"))

        SendMoneyToOtherCountry.formRule.validate(map) must be(Valid(SendMoneyToOtherCountry(false)))
      }

      "fail validation on missing field" in {

        SendMoneyToOtherCountry.formRule.validate(Map.empty) must be(Invalid(
          Seq( Path \ "money" -> Seq(ValidationError("error.required.msb.send.money")))))
      }

      "successfully write form data" in {

        SendMoneyToOtherCountry.formWrites.writes(SendMoneyToOtherCountry(false)) must be(Map("money" -> Seq("false")))
      }
    }

    "Json Validation" must {

      "Successfully read/write Json data" in {

        SendMoneyToOtherCountry.format.reads(SendMoneyToOtherCountry.format.writes(
          SendMoneyToOtherCountry(false))) must be(JsSuccess(SendMoneyToOtherCountry(false), JsPath \ "money"))

      }
    }
  }
}
