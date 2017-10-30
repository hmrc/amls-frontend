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

package models.businessmatching.updateservice

import jto.validation.{From, Rule, Write}
import jto.validation.forms.UrlFormEncoded
import jto.validation.forms.Rules._


sealed trait ChangeServices

case object ChangeServicesAdd extends ChangeServices
case object ChangeServicesRemove extends ChangeServices
case object ChangeServicesReplace extends ChangeServices

object ChangeServices {
  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, ChangeServices] = From[UrlFormEncoded] { __ =>
    (__ \ "changeServices").read[String].withMessage("error.businessmatching.updateservice.changeservices") map {
      case "add" => ChangeServicesAdd
      case "remove" => ChangeServicesRemove
      case "replace" => ChangeServicesReplace
    }
  }

}
