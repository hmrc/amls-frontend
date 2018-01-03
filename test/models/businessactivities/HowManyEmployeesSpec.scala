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

package models.businessactivities

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

import scala.language.postfixOps

class HowManyEmployeesSpec extends PlaySpec {

  "HowManyEmployees" must {
    "write the model fields to url encoded response" in {

      HowManyEmployees.formWrites.writes(HowManyEmployees(Some("123456789"), Some("12345678"))) must
        be(Map("employeeCount" -> Seq("123456789"),
          "employeeCountAMLSSupervision" -> Seq("12345678")))

    }
  }
}