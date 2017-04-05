package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.renewal.PercentageOfCashPaymentOver15000
import models.renewal.Renewal
import services.{RenewalService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.percentage
import scala.concurrent.Future


@Singleton
class PercentageOfCashPaymentOver15000Controller @Inject()(
                                           val dataCacheConnector: DataCacheConnector,
                                           val authConnector: AuthConnector,
                                           val renewalService: RenewalService
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

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[PercentageOfCashPaymentOver15000](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.renewal.percentage(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <- dataCacheConnector.fetch[Renewal](Renewal.key)
            _ <- renewalService.updateRenewal(renewal.percentageOfCashPaymentOver15000(data))
          } yield redirectDependingOnEdit(edit)
      }
    }
  }

  private def redirectDependingOnEdit(edit: Boolean) = edit match {
    case true => Redirect(routes.SummaryController.get())
    case false => Redirect(routes.ReceiveCashPaymentsController.get())
  }

}
