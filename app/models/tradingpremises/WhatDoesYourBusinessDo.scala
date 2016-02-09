package models.tradingpremises

import models.businessmatching.BusinessActivity
import play.api.data.mapping.{Path, RuleLike, From}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping.forms.Rules._

case class WhatDoesYourBusinessDo(activities : Set[BusinessActivity])

object WhatDoesYourBusinessDo {
  implicit def formRule = From[UrlFormEncoded] { __ =>
    (__ \ "activities").read[Set[BusinessActivity]].fmap(WhatDoesYourBusinessDo.apply _)
  }
}