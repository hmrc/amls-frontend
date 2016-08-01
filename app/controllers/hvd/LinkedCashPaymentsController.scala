package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{InvalidForm, ValidForm, EmptyForm, Form2}
import models.hvd.{LinkedCashPayments, Hvd}
import views.html.hvd.linked_cash_payments

import scala.concurrent.Future

trait LinkedCashPaymentsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetch[Hvd](Hvd.key) map {
          response =>
            val form: Form2[LinkedCashPayments] = (for {
              hvd <- response
              linkedCashPayment <- hvd.linkedCashPayment
            } yield Form2[LinkedCashPayments](linkedCashPayment)).getOrElse(EmptyForm)
            Ok(linked_cash_payments(form, edit))
        }
    }
  }

  def post(edit: Boolean = false) = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request => {
        Form2[LinkedCashPayments](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(linked_cash_payments(f, edit)))
          case ValidForm(_, data) =>
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              _ <- dataCacheConnector.save[Hvd](Hvd.key,
                hvd.linkedCashPayment(data)
              )
            } yield edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.ReceiveCashPaymentsController.get())
            }
        }
      }
    }
  }
}

object LinkedCashPaymentsController extends LinkedCashPaymentsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}