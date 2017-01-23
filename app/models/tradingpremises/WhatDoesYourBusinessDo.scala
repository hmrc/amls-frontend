package models.tradingpremises

import models.DateOfChange
import models.businessmatching.BusinessActivity
import play.api.data.mapping._
import play.api.data.mapping.forms.Rules.{minLength => _, _}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.libs.json.Json
import utils.TraversableValidators.minLengthR

case class WhatDoesYourBusinessDo(activities : Set[BusinessActivity], dateOfChange: Option[DateOfChange] = None)

object WhatDoesYourBusinessDo {

  import utils.MappingUtils.Implicits._

  implicit val formRule : Rule[UrlFormEncoded, WhatDoesYourBusinessDo] = From[UrlFormEncoded] { __ =>
    (__ \ "activities")
      .read(minLengthR[Set[BusinessActivity]](1).withMessage("error.required.tp.activity.your.business.do"))
      .fmap((activities) => WhatDoesYourBusinessDo(activities, None))
  }

  implicit val formWrite = Write[WhatDoesYourBusinessDo, UrlFormEncoded] { data =>
    Map("activities[]" -> data.activities.toSeq.map(BusinessActivity.activityFormWrite.writes))
  }

  implicit val format = Json.format[WhatDoesYourBusinessDo]
}
