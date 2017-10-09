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
import jto.validation.{From, Rule, Write}
import play.api.libs.json.Json
import utils.TraversableValidators.minLengthR


case class TradingPremisesActivities(index: Set[Int])

object TradingPremisesActivities {

  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  implicit def formReads: Rule[UrlFormEncoded, TradingPremisesActivities] = From[UrlFormEncoded] { __ =>
      (__ \ "tradingPremises")
        .read(minLengthR[Set[Int]](1).withMessage("error.businessmatching.updateservice.tradingpremises"))
        .flatMap(TradingPremisesActivities.apply)
    }

  implicit def formWrites(implicit w: Write[String, String]) = Write[TradingPremisesActivities, UrlFormEncoded] { data =>
    Map("tradingPremises[]" -> data.index.toSeq.map(x => w.writes(x.toString)))
  }

  implicit def format = Json.format[TradingPremisesActivities]

}