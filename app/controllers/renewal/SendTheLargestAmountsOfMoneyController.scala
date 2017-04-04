package controllers.renewal

import javax.inject.{Inject, Singleton}

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{SendTheLargestAmountsOfMoney, MoneyServiceBusiness}
import services.{RenewalService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.msb.send_largest_amounts_of_money

import scala.concurrent.Future

@Singleton
class SendTheLargestAmountsOfMoneyController @Inject()(
                                            val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector,
                                            val renewalService: RenewalService
                                          ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ControllerHelper.allowedToEdit flatMap {
        case true => dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
          response =>
            val form: Form2[SendTheLargestAmountsOfMoney] = (for {
              msb <- response
              amount <- msb.sendTheLargestAmountsOfMoney
            } yield Form2[SendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
            Ok(send_largest_amounts_of_money(form, edit))
        }
        case false => Future.successful(NotFound(notFoundView))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ???
  }
}
