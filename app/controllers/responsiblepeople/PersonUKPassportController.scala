package controllers.responsiblepeople

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

class PersonUKPassportController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector
                                          ) extends RepeatingSection with BaseController {


  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        ???
  }

  def post(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        ???
  }

}
