package controllers.renewal

import javax.inject.{Inject, Singleton}

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.renewal.PercentageOfCashPaymentOver15000
import models.renewal.Renewal
import services.{RenewalService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.hvd.{receiving, percentage}
import scala.concurrent.Future


@Singleton
class PercentageOfCashPaymentOver15000Controller @Inject()(
                                           val dataCacheConnector: DataCacheConnector,
                                           val authConnector: AuthConnector,
                                           val renewalService: RenewalService,
                                           val statusService: StatusService
                                         ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Renewal](Renewal.key) map {
        response =>
          val form: Form2[PercentageOfCashPaymentOver15000] = (for {
            renewal <- response
            percentageOfCashPaymentOver15000 <- renewal.percentageOfCashPaymentOver15000
          } yield Form2[PercentageOfCashPaymentOver15000](percentageOfCashPaymentOver15000)).getOrElse(EmptyForm)
          Ok(percentage(form, edit))
      }

  }

    def post(edit: Boolean = false) = ???

}
