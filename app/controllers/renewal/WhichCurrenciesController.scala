package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class WhichCurrenciesController @Inject()(
                                           val dataCacheConnector: DataCacheConnector,
                                           val authConnector: AuthConnector,
                                           val renewalService: RenewalService
                                         ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
    ???
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
    ???
  }

}
