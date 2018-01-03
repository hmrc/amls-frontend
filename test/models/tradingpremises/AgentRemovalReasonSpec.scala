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

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class AgentRemovalReasonSpec extends PlaySpec {

  import models.tradingpremises.RemovalReasonConstants._

  "The AgentRemovalReason model" when {

    "given a valid form" must {

      "return the model" when {

        "given 'Other' as a reason" in {

          val form = Map(
            "removalReason" -> Seq(Form.OTHER),
            "removalReasonOther" -> Seq("Some other reason")
          )

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Valid(AgentRemovalReason("Other", Some("Some other reason"))))

        }

        "something other than 'Other' was given as the reason" in {

          val form = Map(
            "removalReason" -> Seq("01")
          )

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Valid(AgentRemovalReason("Serious compliance failures")))

        }
      }
    }

    "given an invalid form" must {

      "fail validation" when {

        "there is no removal reason specified" in {

          val form = Map.empty[String, Seq[String]]

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Invalid(Seq((Path \ "removalReason", Seq(ValidationError("tradingpremises.remove_reasons.missing"))))))

        }

        "there is no other removal reason when 'Other' is selected" in {

          val form = Map("removalReason" -> Seq(Form.OTHER))

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Invalid(Seq(Path \ "removalReasonOther" -> Seq(ValidationError("error.required")))))

        }

        "the given reason for 'Other' is too long" in {

          val form = Map(
            "removalReason" -> Seq(Form.OTHER),
            "removalReasonOther" -> Seq("a" * 300)
          )

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Invalid(Seq(Path \ "removalReasonOther" -> Seq(ValidationError("error.invalid.maxlength.255")))))

        }

      }

    }

    "given a valid model" must {

      "return the form" in {

        val model = AgentRemovalReason("Other", Some("Some other reason"))

        val result = AgentRemovalReason.formWriter.writes(model)

        result must be(Map(
          "removalReason" -> Seq(Form.OTHER),
          "removalReasonOther" -> Seq("Some other reason")
        ))

      }

      "return the correct json" in {
        val model = AgentRemovalReason("Other", Some("Some other reason"))

        Json.toJson(model) must be(Json.obj(
          "removalReason" -> Schema.OTHER,
          "removalReasonOther" -> "Some other reason"
        ))
      }

    }

    "given valid json" must {

      val json =
        """
          | {
          | "removalReason": "Other",
          | "removalReasonOther": "Some reason"
          | }
        """.stripMargin

      "return the model" in {

        Json.parse(json).asOpt[AgentRemovalReason] must be(Some(AgentRemovalReason("Other", Some("Some reason"))))

      }

    }

  }

}
