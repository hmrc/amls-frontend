package controllers.declaration

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController

trait AddPersonController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    ???
  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }

  object AddPersonController extends AddPersonController {
    override val dataCacheConnector = DataCacheConnector
    override protected def authConnector = AMLSAuthConnector
  }

}
