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
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class TotalThroughputSpec extends PlaySpec with MustMatchers {

  "The MsbThroughput model" must {

    "be able to be serialized and deserialized" in {
      val model = TotalThroughput("01")

      Json.fromJson[TotalThroughput](Json.toJson(model)).asOpt mustBe Some(model)
    }

    "fail form validation" when {
      "nothing on the form is selected" in {

        val form = Map.empty[String, Seq[String]]

        TotalThroughput.formReader.validate(form) must be(
          Invalid(Seq(
            (Path \ "throughput") -> Seq(ValidationError("renewal.msb.throughput.selection.required"))
          ))
        )

      }

      "an invalid value is sent" in {
        val form = Map(
          "throughput" -> Seq("some invalid value")
        )

        TotalThroughput.formReader.validate(form) must be(
          Invalid(Seq(
            (Path \ "throughput") -> Seq(ValidationError("renewal.msb.throughput.selection.invalid"))
          ))
        )
      }
    }

    "pass validation" when {
      "the correct value is passed through the form" in {
        val form = Map(
          "throughput" -> Seq("01")
        )

        TotalThroughput.formReader.validate(form) must be(
          Valid(TotalThroughput("01"))
        )
      }
    }

    "write to the form correctly" in {
      val model = TotalThroughput("01")

      TotalThroughput.formWriter.writes(model) must be(
        Map("throughput" -> Seq("01"))
      )
    }

  }
}
