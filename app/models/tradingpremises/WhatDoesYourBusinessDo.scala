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

package models.tradingpremises

import models.DateOfChange
import models.businessmatching.BusinessActivity
import jto.validation._
import jto.validation.forms.Rules.{minLength => _, _}
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json.Json
import utils.TraversableValidators.minLengthR

case class WhatDoesYourBusinessDo(activities : Set[BusinessActivity], dateOfChange: Option[DateOfChange] = None)

object WhatDoesYourBusinessDo {

  import utils.MappingUtils.Implicits._

  implicit val formRule : Rule[UrlFormEncoded, WhatDoesYourBusinessDo] = From[UrlFormEncoded] { __ =>
    (__ \ "activities")
      .read(minLengthR[Set[BusinessActivity]](1).withMessage("error.required.tp.activity.your.business.do"))
      .map((activities) => WhatDoesYourBusinessDo(activities, None))
  }

  implicit val formWrite = Write[WhatDoesYourBusinessDo, UrlFormEncoded] { data =>
    Map("activities[]" -> data.activities.toSeq.map(BusinessActivity.activityFormWrite.writes))
  }

  implicit val format = Json.format[WhatDoesYourBusinessDo]
}
