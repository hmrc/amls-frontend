package controllers.supervision

import javax.inject.Inject

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class WhichProfessionalBodyController @Inject()(
                                               val dataCacheConnector: DataCacheConnector,
                                               val authConnector: AuthConnector = AMLSAuthConnector
                                               ) extends BaseController {

  def get() = Authorised.async{
    implicit authContext =>
      implicit request =>
      ???
  }

  def post() = Authorised.async{
    implicit authContext =>
      implicit request =>
      ???
  }

}
