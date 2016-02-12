package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController

trait BankAccountTypeController extends BaseController {
  val dataCacheConnector : DataCacheConnector

  /*def get(edit:Boolean = false) = Authorised.async {

  }*/

}

object BankAccountTypeController extends BankAccountTypeController {
    override val authConnector = AMLSAuthConnector
    override val dataCacheConnector = DataCacheConnector
}