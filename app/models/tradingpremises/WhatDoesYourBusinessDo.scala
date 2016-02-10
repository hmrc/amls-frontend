package models.tradingpremises

import models.businessmatching.BusinessActivity
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.forms.Rules._
import play.api.data.mapping.forms.Writes._
import play.api.libs.json.{Json, Reads}


case class WhatDoesYourBusinessDo(activities : Set[BusinessActivity])

object WhatDoesYourBusinessDo {
  implicit val formRule = From[UrlFormEncoded] { __ =>
    (__ \ "activities").read[Set[BusinessActivity]].fmap(WhatDoesYourBusinessDo.apply _)
  }

  implicit val formWrite = Write[WhatDoesYourBusinessDo, UrlFormEncoded] { data =>
    Map("activities" -> data.activities.toSeq.map(BusinessActivity.activityFormWrite.writes))
  }

  implicit val format = Json.format[WhatDoesYourBusinessDo]
}