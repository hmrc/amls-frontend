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

package models.tradingpremises

import models.responsiblepeople.PremisesRegistered
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class PremisesRegisteredSpec extends PlaySpec with MockitoSugar {

  "PremisesRegistered" must {

    "validate the given model" in {
      val data = Map(
        "registerAnotherPremises" -> Seq("true")
      )

      PremisesRegistered.formRule.validate(data) must
        be(Valid(PremisesRegistered(true)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "registerAnotherPremises" -> Seq("true")
      )

      PremisesRegistered.formRule.validate(data) must
        be(Valid(PremisesRegistered(true)))
    }

    "fail to validate when given invalid data" in {

      PremisesRegistered.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "registerAnotherPremises") -> Seq(ValidationError("error.required.tp.register.another.premises"))
        )))
    }

    "write correct data" in {

      val model = PremisesRegistered(true)

      PremisesRegistered.formWrites.writes(model) must
        be(Map(
          "registerAnotherPremises" -> Seq("true")
        ))
    }
  }
}
