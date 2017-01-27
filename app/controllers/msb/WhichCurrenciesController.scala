package controllers.msb

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness._
import play.api.Logger
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.msb.which_currencies

import scala.concurrent.Future

trait WhichCurrenciesController extends BaseController {

  def cache: DataCacheConnector
  implicit val statusService: StatusService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      ControllerHelper.allowedToEdit flatMap {
        case true => cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>
          val form = (for {
            msb <- response
            currencies <- msb.whichCurrencies
          } yield Form2[WhichCurrencies](currencies)).getOrElse(EmptyForm)

          Ok(views.html.msb.which_currencies(form, edit))
      }
        case false => Future.successful(NotFound(notFoundView))
      }
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      val foo = Form2[WhichCurrencies](request.body)
      foo match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.which_currencies(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.whichCurrencies(data)
            )

          } yield Redirect(routes.SummaryController.get())
      }
    }
  }
}

object WhichCurrenciesController extends WhichCurrenciesController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val cache = DataCacheConnector
  override val statusService: StatusService = StatusService
}
