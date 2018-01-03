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

package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class PersonRegisteredSpec extends PlaySpec with MockitoSugar {

  "PersonRegistered" must {

    "validate the given model" in {
      val data = Map(
        "registerAnotherPerson" -> Seq("false")
      )

      PersonRegistered.formRule.validate(data) must
        be(Valid(PersonRegistered(false)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "registerAnotherPerson" -> Seq("true")
      )

      PersonRegistered.formRule.validate(data) must
        be(Valid(PersonRegistered(true)))
    }

    "fail to validate when given invalid data" in {

      PersonRegistered.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "registerAnotherPerson") -> Seq(ValidationError("error.required.rp.register.another.person"))
        )))
    }

    "write correct data" in {

      val model = PersonRegistered(true)

      PersonRegistered.formWrites.writes(model) must
        be(Map(
          "registerAnotherPerson" -> Seq("true")
        ))
    }
  }
}
