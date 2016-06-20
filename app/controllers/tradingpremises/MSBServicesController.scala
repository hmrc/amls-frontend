package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.{TradingPremises, MsbServices}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait MSBServicesController extends BaseController {

  def cache: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      cache.fetch[TradingPremises](TradingPremises.key) map {
        response =>
          val form = (for {
            msb <- response
            services <- msb.msbServices
          } yield Form2[MsbServices](services)).getOrElse(EmptyForm)

          Ok(views.html.msb.services(form, edit))
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.services(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- cache.fetch[TradingPremises](TradingPremises.key)
             _ <- cache.save[TradingPremises](TradingPremises.key,
              msb.msbServices(data)
            )
          } yield Redirect(routes.SummaryController.get())
      }
  }
}

object MSBServicesController extends MSBServicesController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val cache = DataCacheConnector
}
