package controllers.businessmatching.updateservice

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class FitAndProperController @Inject()(val authConnector: AuthConnector,
                                                 val dataCacheConnector: DataCacheConnector,
                                                 val businessMatchingService: BusinessMatchingService)() extends BaseController {

  def get() = Authorised.async{
    implicit request => implicit authContext =>
      ???
  }


  def post() = Authorised.async{
    implicit request => implicit authContext =>
      ???
  }


}