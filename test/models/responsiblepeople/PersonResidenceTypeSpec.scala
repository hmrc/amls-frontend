/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.responsiblepeople.NinoUtil
import models.Country
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.Nino

class PersonResidenceTypeSpec extends PlaySpec with NinoUtil {

  "PersonResidenceType" when {

    "validating JSON" must {

      "read non-uk residence type in old format" in {
        val json = Json.obj(
          "isUKResidence"       -> false,
          "dateOfBirth"         -> "1990-10-02",
          "nonUKPassportNumber" -> "87654321",
          "countryOfBirth"      -> "GB",
          "nationality"         -> "GB"
        )

        json.as[PersonResidenceType] must be(
          PersonResidenceType(
            NonUKResidence,
            Some(Country("United Kingdom", "GB")),
            Some(Country("United Kingdom", "GB"))
          )
        )
      }

      "read uk residence type model" in {
        val ukModel = PersonResidenceType(
          UKResidence(Nino("AA346464A")),
          Some(Country("United Kingdom", "GB")),
          Some(Country("United Kingdom", "GB"))
        )

        PersonResidenceType.jsonRead.reads(PersonResidenceType.jsonWrite.writes(ukModel)) must
          be(JsSuccess(ukModel))

      }

      "validate non uk residence type model" in {
        val nonUKModel = PersonResidenceType(
          NonUKResidence,
          Some(Country("United Kingdom", "GB")),
          Some(Country("United Kingdom", "GB"))
        )

        PersonResidenceType.jsonRead.reads(PersonResidenceType.jsonWrite.writes(nonUKModel)) must
          be(JsSuccess(nonUKModel))
      }
    }
  }
}
