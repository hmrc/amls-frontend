package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.hvd.{ReceiveCashPayments, Hvd, PercentageOfCashPaymentOver15000}
import services.StatusService
import utils.ControllerHelper
import views.html.hvd.{receiving, percentage}
import scala.concurrent.Future

trait PercentageOfCashPaymentOver15000Controller extends BaseController {

  val dataCacheConnector: DataCacheConnector
  implicit val statusService: StatusService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true =>
          dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[PercentageOfCashPaymentOver15000] = (for {
              hvd <- response
              percentageOfCashPaymentOver15000 <- hvd.percentageOfCashPaymentOver15000
            } yield Form2[PercentageOfCashPaymentOver15000](percentageOfCashPaymentOver15000)).getOrElse(EmptyForm)
            Ok(percentage(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

    def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[PercentageOfCashPaymentOver15000](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(percentage(f, edit)))
        case ValidForm(_, data) =>
          for {
            hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
            _ <- dataCacheConnector.save[Hvd](Hvd.key,
              hvd.percentageOfCashPaymentOver15000(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}

object PercentageOfCashPaymentOver15000Controller extends PercentageOfCashPaymentOver15000Controller {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService: StatusService = StatusService
}
