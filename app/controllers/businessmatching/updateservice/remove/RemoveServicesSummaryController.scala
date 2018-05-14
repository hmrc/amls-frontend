package controllers.businessmatching.updateservice.remove

import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.Inject
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class RemoveServicesSummaryController @Inject()(
                                                 val authConnector: AuthConnector,
                                                 val dataCacheConnector: DataCacheConnector
                                               ) extends BaseController {

  def get = Authorised.async{
    implicit authContext =>
      implicit request => ???
  }

  def post = Authorised.async{
    implicit authContext =>
      implicit request => ???
  }

}

}
