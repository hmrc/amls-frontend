package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.hvd.{CashPayment, Hvd}
import views.html.hvd.cash_payment

import scala.concurrent.Future

trait CashPaymentController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Hvd](Hvd.key) map {
        response =>
          val form: Form2[CashPayment] = (for {
            hvd <- response
            cashPayment <- hvd.cashPayment
          } yield Form2[CashPayment](cashPayment)).getOrElse(EmptyForm)
          Ok(cash_payment(form, edit))
      }
  }


  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[CashPayment](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(cash_payment(f, edit)))
        case ValidForm(_, data) =>
          for {
            hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
            _ <- dataCacheConnector.save[Hvd](Hvd.key,
              hvd.cashPayment(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.CashPaymentController.get())
          }
      }
    }
  }
}

object CashPaymentController extends CashPaymentController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
