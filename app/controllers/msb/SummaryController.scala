package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.moneyservicebusiness.MoneyServiceBusiness
import play.api.Logger
import views.html.msb.summary

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        case Some(data) =>
          Logger.debug(s"----------------------------------------->>>>>>>>>$data")
          Ok(summary(data))
        case _ =>
          Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
