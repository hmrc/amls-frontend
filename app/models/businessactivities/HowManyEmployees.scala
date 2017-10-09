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

import jto.validation.forms._
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.Json
import models.FormTypes._

import jto.validation.forms.Rules._
import utils.MappingUtils.Implicits._

case class HowManyEmployees(employeeCount: Option[String] = None,
                            employeeCountAMLSSupervision: Option[String] = None)


object HowManyEmployees {

  implicit val formats = Json.format[HowManyEmployees]


  implicit val formWrites: Write[HowManyEmployees, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "employeeCount").write[Option[String]] ~
        (__ \ "employeeCountAMLSSupervision").write[Option[String]]
      ) (unlift(HowManyEmployees.unapply _))
  }
}
