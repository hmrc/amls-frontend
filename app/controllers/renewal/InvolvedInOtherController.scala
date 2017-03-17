package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class InvolvedInOtherController @Inject()(
                                         val dataCacheConnector: DataCacheConnector,
                                         val authConnector: AuthConnector,
                                         val statusService: StatusService
                                         ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    ???
  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }
}



