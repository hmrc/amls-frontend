package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController

trait ActivityStartDateController extends BaseController {
   def dataCache: DataCacheConnector

}

object ActivityStartDateController extends ActivityStartDateController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
