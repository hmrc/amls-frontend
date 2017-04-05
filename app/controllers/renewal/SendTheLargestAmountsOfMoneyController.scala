package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.renewal.{Renewal, SendTheLargestAmountsOfMoney}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.send_largest_amounts_of_money

@Singleton
class SendTheLargestAmountsOfMoneyController @Inject()(
                                                        val dataCacheConnector: DataCacheConnector,
                                                        val authConnector: AuthConnector,
                                                        val renewalService: RenewalService
                                                      ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Renewal](Renewal.key) map {
        response =>
          val form: Form2[SendTheLargestAmountsOfMoney] = (for {
            renewal <- response
            amount <- renewal.sendTheLargestAmountsOfMoney
          } yield Form2[SendTheLargestAmountsOfMoney](amount)).getOrElse(EmptyForm)
          Ok(send_largest_amounts_of_money(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      ???
  }
}
