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

import jto.validation.{From, Rule, To}
import jto.validation.forms.UrlFormEncoded
import models.businessmatching.BusinessActivity

case class TradingPremisesSubmittedActivities(allPremises: Boolean, activity: BusinessActivity)

object TradingPremisesSubmittedActivities {
  import jto.validation._
  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, TradingPremisesSubmittedActivities] = From[UrlFormEncoded] { __ =>
    (
      (__ \ "allPremises").read[Boolean].withMessage("error.businessmatching.updateservice.tradingpremisessubmittedactivities") ~
        (__ \ "activity").read[BusinessActivity]
    )(TradingPremisesSubmittedActivities.apply)
  }

  implicit val formWriter: Write[TradingPremisesSubmittedActivities, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "allPremises").write[Boolean] ~
        (__ \ "activity").write[BusinessActivity]
    )(unlift(TradingPremisesSubmittedActivities.unapply))
  }

}
