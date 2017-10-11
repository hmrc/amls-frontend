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
import play.api.libs.json._

sealed trait AreSubmittedActivitiesAtTradingPremises
case object SubmittedActivitiesAtTradingPremisesYes extends AreSubmittedActivitiesAtTradingPremises
case object SubmittedActivitiesAtTradingPremisesNo extends AreSubmittedActivitiesAtTradingPremises

object AreSubmittedActivitiesAtTradingPremises {
  import jto.validation._
  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, AreSubmittedActivitiesAtTradingPremises] = From[UrlFormEncoded] { __ =>
  (__ \ "submittedActivities").read[Boolean].withMessage("error.businessmatching.updateservice.tradingpremisessubmittedactivities") map {
      case true => SubmittedActivitiesAtTradingPremisesYes
      case _ => SubmittedActivitiesAtTradingPremisesNo
    }
  }

  implicit val formWriter = Write[AreSubmittedActivitiesAtTradingPremises, UrlFormEncoded] { m =>
    Map("submittedActivities" -> Seq(m match {
      case SubmittedActivitiesAtTradingPremisesYes => "true"
      case _ => "false"
    }))
  }

  implicit val jsonReads: Reads[AreSubmittedActivitiesAtTradingPremises] =
    (__ \ "submittedActivities").read[Boolean] map {
      case true => SubmittedActivitiesAtTradingPremisesYes
      case false => SubmittedActivitiesAtTradingPremisesNo
    }

  implicit val jsonWrites = Writes[AreSubmittedActivitiesAtTradingPremises] {
    case SubmittedActivitiesAtTradingPremisesYes => Json.obj("submittedActivities" -> true)
    case SubmittedActivitiesAtTradingPremisesNo => Json.obj("submittedActivities" -> false)
  }
}
