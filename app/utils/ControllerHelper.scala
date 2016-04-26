package utils

import models.businessmatching.{BusinessType, BusinessMatching}

object ControllerHelper {
  def getBusinessType(matching: Option[BusinessMatching]): Option[BusinessType] = {
    matching flatMap { bm =>
      bm.reviewDetails match {
        case Some(review) => review.businessType
        case _ => None
      }
    }
  }
}
