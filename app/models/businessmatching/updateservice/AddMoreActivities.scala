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

package models.businessmatching.updateservice

import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded

sealed trait AddMoreActivities

case object AddMoreActivitiesYes extends AddMoreActivities
case object AddMoreActivitiesNo extends AddMoreActivities


object AddMoreActivities {
  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, AddMoreActivities] = From[UrlFormEncoded] { __ =>
    (__ \ "AddMoreActivities").read[String].withMessage("error.businessmatching.updateservice.changeservices") map {
      case "yes" => AddMoreActivitiesYes
      case "no" => AddMoreActivitiesNo

    }
  }

}
