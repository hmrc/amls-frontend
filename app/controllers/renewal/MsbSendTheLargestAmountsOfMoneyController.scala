package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{MsbSendTheLargestAmountsOfMoney, Renewal}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal.send_largest_amounts_of_money

import scala.concurrent.Future

@Singleton
class MsbSendTheLargestAmountsOfMoneyController @Inject()(
                                                        val dataCacheConnector: DataCacheConnector,
                                                        val authConnector: AuthConnector,
                                                        val renewalService: RenewalService
                                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Renewal](Renewal.key) map {
        response =>
          val form: Form2[MsbSendTheLargestAmountsOfMoney] = (for {
            renewal <- response
            amount <- renewal.sendTheLargestAmountsOfMoney
          } yield Form2[MsbSendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
          Ok(send_largest_amounts_of_money(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbSendTheLargestAmountsOfMoney](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(send_largest_amounts_of_money(f, edit)))
        case ValidForm(_, data) =>
          for {
            renewal <-
            dataCacheConnector.fetch[Renewal](Renewal.key)
            _ <- renewalService.updateRenewal(renewal.sendTheLargestAmountsOfMoney(data))
          } yield redirectDependingOnEdit(edit)
      }
  }

  def redirectDependingOnEdit(edit:Boolean) = edit match {
    case true  => Redirect(routes.SummaryController.get())
    case _ => Redirect(routes.MsbMostTransactionsController.get(edit))
  }
}
