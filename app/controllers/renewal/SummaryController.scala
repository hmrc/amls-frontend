package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.summary


@Singleton
class SummaryController @Inject()(
                                           val dataCacheConnector: DataCacheConnector,
                                           val authConnector: AuthConnector,
                                           val renewalService: RenewalService
                                         ) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      for {
        renewal <- renewalService.getRenewal
      } yield renewal match {
        case Some(data) => Ok(summary(data))
        case _ => Redirect(controllers.renewal.routes.RenewalProgressController.get())
      }
  }
}
