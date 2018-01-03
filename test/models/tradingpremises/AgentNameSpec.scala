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

import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}
import play.api.test.FakeApplication

class AgentNameSpec extends PlaySpec with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> false))

  "AgentName" must {

    "validate form Read" in {
      val formInput = Map(
        "agentName" -> Seq("sometext")
      )

      AgentName.formReads.validate(formInput) must be(Valid(AgentName("sometext", None)))
    }

    "throw error when required name field is missing" in {
      val formInput = Map("agentName" -> Seq(""))
      AgentName.formReads.validate(formInput) must be(Invalid(Seq((Path \ "agentName", Seq(ValidationError("error.required.tp.agent.name"))))))
    }

    "throw error when name input exceeds max length" in {
      val formInput = Map("agentName" -> Seq("sometesttexttest" * 11))
      AgentName.formReads.validate(formInput) must be(Invalid(Seq((Path \ "agentName") -> Seq(ValidationError("error.invalid.tp.agent.name")))))
    }

    "throw error when name input has invalid data" in {
      val formInput = Map("agentName" -> Seq("<sometesttexttest>"))
      AgentName.formReads.validate(formInput) must be(Invalid(Seq((Path \ "agentName") -> Seq(ValidationError("err.text.validation")))))
    }

    "validate form write" in {
      AgentName.formWrites.writes(AgentName("sometext")) must be(Map("agentName" -> Seq("sometext")))
    }

  }

  "Json Validation" must {
    "Successfully read/write Json data" in {
      AgentName.format.reads(AgentName.format.writes(
        AgentName("test", Some(DateOfChange(new LocalDate(2017, 1, 1)))))) must be(
        JsSuccess(
          AgentName("test", Some(DateOfChange(new LocalDate(2017, 1, 1))))))
    }

    "Succesfully read/write Json data with agent dob" in {

      AgentName.format.reads(AgentName.format.writes(
        AgentName("test", Some(DateOfChange(new LocalDate(2017, 1, 1))), Some(new LocalDate(2015, 10, 10))))) must be(
        JsSuccess(
          AgentName("test", Some(DateOfChange(new LocalDate(2017, 1, 1))), Some(new LocalDate(2015, 10, 10)))))

    }

  }
}

class AgentNameSpecR7 extends PlaySpec with OneAppPerSuite {

  override lazy val app = FakeApplication(additionalConfiguration = Map("microservice.services.feature-toggle.release7" -> true))

  "AgentName" must {

    "validate form Read with agent dob" in {

      val formInput = Map(
        "agentName" -> Seq("sometext"),
        "agentDateOfBirth.day" -> Seq("15"),
        "agentDateOfBirth.month" -> Seq("2"),
        "agentDateOfBirth.year" -> Seq("1956")
      )

      AgentName.formReads.validate(formInput) must be(Valid(AgentName("sometext", None, Some(new LocalDate("1956-02-15")))))

    }



    "throw error when required date of birth field is missing" in {
      val noContentModel = Map("agentName" -> Seq("sometext")) ++ Map(
        "agentDateOfBirth.day" -> Seq(""),
        "agentDateOfBirth.month" -> Seq(""),
        "agentDateOfBirth.year" -> Seq("")
      )

      AgentName.formReads.validate(noContentModel) must be(Invalid(Seq((Path \ "agentDateOfBirth", Seq(ValidationError("error.expected.jodadate.format", "yyyy-MM-dd"))))))
    }
  }

}
