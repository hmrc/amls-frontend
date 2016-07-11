package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.hvd.{Hvd, ReceiveCashPayments}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.hvd.receiving

import scala.concurrent.Future

trait ReceiveCashPaymentsController extends BaseController {

  def cacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        cacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[ReceiveCashPayments] = (for {
              hvd <- response
              receivePayments <- hvd.receiveCashPayments
            } yield Form2[ReceiveCashPayments](receivePayments)).getOrElse(EmptyForm)
            Ok(receiving(form, edit))
        }
    }
  }

  def post(edit: Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request => {
        Form2[ReceiveCashPayments](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(receiving(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- cacheConnector.fetch[Hvd](Hvd.key)
              _ <- cacheConnector.save[Hvd](Hvd.key,
                hvd.receiveCashPayments(data)
              )
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.LinkedCashPaymentsController.get())
            }
        }
      }
    }
  }

}

object ReceiveCashPaymentsController extends ReceiveCashPaymentsController {
  override val cacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
