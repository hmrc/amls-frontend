package controllers.responsiblepeople

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class TimeAtAddressController @Inject()(
                                              val dataCacheConnector: DataCacheConnector,
                                              val authConnector: AuthConnector
                                            ) extends BaseController {

  def get(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    ???
  }

  def post(index: Int, edit: Boolean = false, fromDeclaration: Boolean = false) = Authorised.async {
    ???
  }

}