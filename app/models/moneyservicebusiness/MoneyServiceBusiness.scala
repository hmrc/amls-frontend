package models.moneyservicebusiness

import models.registrationprogress.{Completed, NotStarted, Started, Section}
import typeclasses.MongoKey
import uk.gov.hmrc.http.cache.client.CacheMap
import play.api.libs.json._

case class MoneyServiceBusiness(msbServices : Option[MsbServices] = None) {
  def isComplete = msbServices.nonEmpty
}

object MoneyServiceBusiness{
  implicit def default(value : Option[MoneyServiceBusiness]) :  MoneyServiceBusiness = {
    value.getOrElse(MoneyServiceBusiness())
  }

  val key = "money-service-business"

  implicit val mongoKey = new MongoKey[MoneyServiceBusiness] {
    def apply() = "money-service-business"
  }

  def section(implicit cache: CacheMap): Section = {
    val messageKey = "money_service_business"
    val notStarted = Section(messageKey, NotStarted, controllers.msb.routes.WhatYouNeedController.get())
    cache.getEntry[MoneyServiceBusiness](key).fold(notStarted) {
      model =>
        if (model.isComplete) {
          Section(messageKey, Completed, controllers.msb.routes.WhatYouNeedController.get())
        } else {
          Section(messageKey, Started, controllers.msb.routes.WhatYouNeedController.get())
        }
    }
  }

  implicit val jsonReads : Reads[MoneyServiceBusiness] = Reads[MoneyServiceBusiness] { jsVal =>
    Json.fromJson[MsbServices](jsVal).map(x => MoneyServiceBusiness(Some(x)))
  }

  implicit val jsonWrites : Writes[MoneyServiceBusiness] = Writes { msb:MoneyServiceBusiness =>
    Json.toJson(msb.msbServices)
  }
}


