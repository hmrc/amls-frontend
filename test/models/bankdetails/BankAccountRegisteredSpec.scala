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

package models.bankdetails

import models.responsiblepeople.BankAccountRegistered
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class BankAccountRegisteredSpec extends PlaySpec with MockitoSugar {

  "BankAccountRegistered" must {

    "validate the given model" in {
      val data = Map(
        "registerAnotherBank" -> Seq("true")
      )

      BankAccountRegistered.formRule.validate(data) must
        be(Valid(BankAccountRegistered(true)))
    }

    "successfully validate given a data model containing true" in {

      val data = Map(
        "registerAnotherBank" -> Seq("true")
      )

      BankAccountRegistered.formRule.validate(data) must
        be(Valid(BankAccountRegistered(true)))
    }

    "successfully validate given a data model containing false" in {

      val data = Map(
        "registerAnotherBank" -> Seq("false")
      )

      BankAccountRegistered.formRule.validate(data) must
        be(Valid(BankAccountRegistered(false)))
    }

    "fail to validate when given invalid data" in {

      BankAccountRegistered.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "registerAnotherBank") -> Seq(ValidationError("error.required.bankdetails.register.another.bank"))
        )))
    }

    "write correct data" in {

      val model = BankAccountRegistered(true)

      BankAccountRegistered.formWrites.writes(model) must
        be(Map(
          "registerAnotherBank" -> Seq("true")
        ))
    }
  }
}
