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

import jto.validation.forms.UrlFormEncoded
import models.businessmatching.BusinessActivity

sealed trait TradingPremisesNewActivities

case class TradingPremisesNewActivitiesYes(activity: BusinessActivity) extends TradingPremisesNewActivities
case object TradingPremisesNewActivitiesNo extends TradingPremisesNewActivities

object TradingPremisesNewActivities {

  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, TradingPremisesNewActivities] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "tradingPremisesNewActivities").read[Boolean].withMessage("error.businessmatching.updateservice.tradingpremisesnewactivities") flatMap {
      case true => {
        (__ \ "businessActivities").read[BusinessActivity] map TradingPremisesNewActivitiesYes.apply
      }
      case false => TradingPremisesNewActivitiesNo
    }
  }

  implicit val formWrites: Write[TradingPremisesNewActivities, UrlFormEncoded] = Write {
    case TradingPremisesNewActivitiesYes(_) => "tradingPremisesNewActivities" -> "true"
    case TradingPremisesNewActivitiesNo => "tradingPremisesNewActivities" -> "false"
  }
}
