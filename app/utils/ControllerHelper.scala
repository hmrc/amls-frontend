package utils

import models.businessmatching.{BusinessActivities, BusinessMatching, BusinessType, MoneyServiceBusiness}

object ControllerHelper {

  def getBusinessType(matching: Option[BusinessMatching]): Option[BusinessType] = {
    matching flatMap { bm =>
      bm.reviewDetails match {
        case Some(review) => review.businessType
        case _ => None
      }
    }
  }

  def getBusinessActivity(matching: Option[BusinessMatching]): Option[BusinessActivities] = {
    matching match {
      case Some(data) => data.activities
      case None => None
    }
  }

  def isMSBSelected(bm: Option[BusinessMatching]): Boolean = {
    bm match {
      case Some(matching) => matching.activities.foldLeft(false) { (x, y) =>
        y.businessActivities.contains(MoneyServiceBusiness)
      }
      case None => false
    }
  }
}
