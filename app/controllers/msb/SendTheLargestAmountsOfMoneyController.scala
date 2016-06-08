package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{SendTheLargestAmountsOfMoney, MoneyServiceBusiness}
import views.html.msb.send_largest_amounts_of_money

import scala.concurrent.Future

trait SendTheLargestAmountsOfMoneyController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form: Form2[SendTheLargestAmountsOfMoney] = (for {
            msb <- response
            amount <- msb.sendTheLargestAmountsOfMoney
          } yield Form2[SendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
          Ok(send_largest_amounts_of_money(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[SendTheLargestAmountsOfMoney](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(send_largest_amounts_of_money(f, edit)))
        case ValidForm(_, data) => {
          for {
            businessActivity <-
            dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              businessActivity.sendTheLargestAmountsOfMoney(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SummaryController.get())
          }
        }
      }
  }
}

object SendTheLargestAmountsOfMoneyController extends SendTheLargestAmountsOfMoneyController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
