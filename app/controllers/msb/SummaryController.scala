package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.moneyservicebusiness.MoneyServiceBusiness
import services.StatusService
import utils.ControllerHelper
import views.html.msb.summary

import scala.concurrent.Future

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector
  implicit val statusService:StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            msb <- cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key)
          } yield {
            ControllerHelper.allowedToEdit map(x => Ok(summary(msb, businessMatching.msbServices,x)))
          }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
