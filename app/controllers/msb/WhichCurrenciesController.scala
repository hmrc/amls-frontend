package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.moneyservicebusiness._
import play.api.Logger
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.which_currencies

import scala.concurrent.Future

trait WhichCurrenciesController extends BaseController {
  def cache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form = (for {
            msb <- response
            currencies <- msb.whichCurrencies
          } yield Form2[WhichCurrencies](currencies)).getOrElse(EmptyForm)

          Ok(views.html.msb.which_currencies(form, edit))
      }
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      val foo = Form2[WhichCurrencies](request.body)
      Logger.debug(s"=============================$foo")
      foo match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.which_currencies(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.whichCurrencies(data)
            )
          // TODO: Go to next page
          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}

object WhichCurrenciesController extends WhichCurrenciesController {
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val cache = DataCacheConnector
}
