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

package models.businessactivities

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json
import models.FormTypes._
import utils.MappingUtils.Implicits._
import jto.validation.forms.Rules._

case class EmployeeCount(employeeCount: String)

object EmployeeCount {

  val employeeCountRegex = "^[0-9]+$".r
  val maxEmployeeCount = 11
  val employeeCountType = notEmptyStrip andThen maxLength(maxEmployeeCount).withMessage("error.max.length.ba.employee.count") andThen
    pattern(employeeCountRegex).withMessage("error.invalid.ba.employee.count")

  implicit val formats = Json.format[EmployeeCount]

  implicit val formRule: Rule[UrlFormEncoded, EmployeeCount] =
    From[UrlFormEncoded] { __ =>
        (__ \ "employeeCount").read(employeeCountType) map EmployeeCount.apply
    }

  implicit val formWrites: Write[EmployeeCount, UrlFormEncoded] =
    Write {
      case EmployeeCount(employeeCount) =>
        Map(
          "employeeCount" -> Seq(employeeCount)
        )
    }
}