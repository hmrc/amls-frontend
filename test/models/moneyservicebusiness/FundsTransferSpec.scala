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

package models.moneyservicebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation._
import jto.validation.forms.UrlFormEncoded

class FundsTransferSpec extends PlaySpec {
  "FundsTransfer" must {

    "Must successfully validate when user selects 'yes' option" in {
      val data: Map[String, Seq[String]] = Map(
      "transferWithoutFormalSystems" -> Seq("true")
      )

      FundsTransfer.formRule.validate(data) must
      be(Valid(FundsTransfer(true)))
    }

    "successfully validate when user selects 'no' option" in {
      val data: UrlFormEncoded = Map(
        "transferWithoutFormalSystems" -> Seq("false")
      )

      FundsTransfer.formRule.validate(data) must
      be(Valid(FundsTransfer(false)))
    }

    "fail validation when data is invalid" in {
      FundsTransfer.formRule.validate(Map.empty) must
        be(Invalid(Seq(
        (Path \ "transferWithoutFormalSystems") -> Seq(ValidationError("error.required.msb.fundsTransfer"))
        )))
    }

    "write correct data" in {

      val model = FundsTransfer(true)

      FundsTransfer.formWrites.writes(model) must
        be(Map(
          "transferWithoutFormalSystems" -> Seq("true")
        ))
    }
  }
}

