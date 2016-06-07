package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{SendMoneyToOtherCountry, MoneyServiceBusiness}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.send_money_to_other_country

import scala.concurrent.Future

trait SendMoneyToOtherCountryController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit:Boolean = false) = Authorised.async {
   implicit authContext => implicit request =>
     dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
       response =>
         val form: Form2[SendMoneyToOtherCountry] = (for {
           msb <- response
           money <- msb.sendMoneyToOtherCountry
         } yield Form2[SendMoneyToOtherCountry](money)).getOrElse(EmptyForm)
         Ok(send_money_to_other_country(form, edit))
     }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[SendMoneyToOtherCountry](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(send_money_to_other_country(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.sendMoneyToOtherCountry(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SummaryController.get())
          }
      }
    }
  }
}

object SendMoneyToOtherCountryController extends SendMoneyToOtherCountryController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
