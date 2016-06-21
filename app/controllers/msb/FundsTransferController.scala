package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.moneyservicebusiness.{CurrencyExchange, TransmittingMoney, FundsTransfer, MoneyServiceBusiness}
import views.html.msb._

import scala.concurrent.Future

trait FundsTransferController extends BaseController {

  def dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[FundsTransfer] = (for {
            moneyServiceBusiness <- response
            fundsTransfer <- moneyServiceBusiness.fundsTransfer
          } yield Form2[FundsTransfer](fundsTransfer)).getOrElse(EmptyForm)
          Ok(funds_transfer(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[FundsTransfer](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(funds_transfer(f, edit)))
        case ValidForm(_, data) =>
          for {
            moneyServiceBusiness <- dataCache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              moneyServiceBusiness.fundsTransfer(data))
          } yield edit match {
            case true if moneyServiceBusiness.transactionsInNext12Months.isDefined =>
              Redirect(routes.SummaryController.get())
            case _ => Redirect(routes.TransactionsInNext12MonthsController.get(edit))
          }
      }
  }
}


object FundsTransferController extends FundsTransferController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
