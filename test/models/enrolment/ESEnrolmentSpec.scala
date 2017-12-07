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

package models.enrolment

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import models.enrolment.Formatters._

class ESEnrolmentSpec extends PlaySpec with MustMatchers {

  "The JSON reads" must {
    "deserialize the Json correctly into the model" in {

      val json = Json.obj(
        "startRecord" -> 1,
        "totalRecords" -> 10,
        "enrolments" -> Seq(
          Json.obj(
            "service" -> "IR-PAYE",
            "state" -> "active",
            "friendlyName" -> "PAYE Enrolment",
            "identifiers" -> Seq(
              Json.obj("key" -> "VRN", "value" -> "VRNVRNVRN"),
              Json.obj("key" -> "PostCode", "value" -> "POSTCODE")
            )
          ),
          Json.obj(
            "service" -> "IR-PAYE",
            "state" -> "active",
            "friendlyName" -> "Second PAYE enrolment",
            "identifiers" -> Seq(
              Json.obj("key" -> "VRN", "value" -> "NRVNRVNRV"),
              Json.obj("key" -> "PostCode", "value" -> "EDOCTSOP")
            )
          )
        )
      )

      json.as[ESEnrolment] mustBe ESEnrolment(
        1,
        10,
        Seq(
          EnrolmentEntry("IR-PAYE", "active", "PAYE Enrolment", Seq(
            EnrolmentIdentifier("VRN", "VRNVRNVRN"),
            EnrolmentIdentifier("PostCode", "POSTCODE")
          )),
          EnrolmentEntry("IR-PAYE", "active", "Second PAYE enrolment", Seq(
            EnrolmentIdentifier("VRN", "NRVNRVNRV"),
            EnrolmentIdentifier("PostCode", "EDOCTSOP")
          ))
        )
      )
    }
  }
}
