package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import play.api.mvc.Action
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

@Singleton
class CustomersOutsideUKController @Inject()(
                                              val dataCacheConnector: DataCacheConnector,
                                              val authConnector: AuthConnector
                                            ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    ???
  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }

}