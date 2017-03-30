package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.renewal.Renewal
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.summary

import scala.concurrent.Future


@Singleton
class SummaryController @Inject()(
                                           val dataCacheConnector: DataCacheConnector,
                                           val authConnector: AuthConnector,
                                           val renewalService: RenewalService
                                         ) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>

      dataCacheConnector.fetchAll flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            renewal <- cache.getEntry[Renewal](Renewal.key)
          } yield {
            Future.successful(Ok(summary(renewal, businessMatching.activities)))
          }) getOrElse {
            Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
          }
      }
  }
}
