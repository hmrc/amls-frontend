package controllers.tradingpremises

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.aboutthebusiness.AboutTheBusiness
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

@Singleton
class ConfirmAddressController @Inject()(override val messagesApi: MessagesApi,
                                         val dataCacheConnector: DataCacheConnector, val authConnector: AuthConnector)
  extends RepeatingSection with BaseController {

  def get(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
    dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
      case Some(atb) => Ok(views.html.tradingpremises.confirm_trading_premises_address(EmptyForm, atb.registeredOffice, index))
      case _ => Ok(views.html.tradingpremises.confirm_trading_premises_address(EmptyForm, index))
    }
  }


}
