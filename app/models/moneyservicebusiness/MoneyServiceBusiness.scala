package models.moneyservicebusiness

import typeclasses.MongoKey

case class MoneyServiceBusiness(msbServices : Option[MsbServices] = None) {
}

object MoneyServiceBusiness{
  implicit def default(value : Option[MoneyServiceBusiness]) :  MoneyServiceBusiness = {
    value.getOrElse(MoneyServiceBusiness())
  }

  implicit val mongoKey = new MongoKey[MoneyServiceBusiness] {
    def apply() = "money-service-business"
  }
}


