package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{CurrencyExchange, MoneyServiceBusiness, MsbServices, TransmittingMoney}
import play.api.mvc.Call
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait ServicesController extends BaseController {

  def cache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>

          val form = (for {
            msb <- response
            services <- msb.msbServices
          } yield Form2[MsbServices](services)).getOrElse(EmptyForm)

          Ok(views.html.msb.services(form, edit))
      }
  }

  // TODO: Correct the routing here once pages exist
  private def route(services: Option[MsbServices], newServices: MsbServices): Call =
    (newServices.services -- services.map(_.services).getOrElse(Set.empty)) match {
      case w if w.contains(TransmittingMoney) =>
        routes.ServicesController.get()
      case w if w.contains(CurrencyExchange) =>
        routes.ServicesController.get()
      case _ =>
        routes.SummaryController.get()
    }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.services(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
             _ <- cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.msbServices(data)
            )
              // TODO: Go to next page
          } yield edit match {
            case false =>
              Redirect(routes.ServicesController.get())
            case true =>
              Redirect(route(msb.msbServices, data))
          }
      }
  }
}

object ServicesController extends ServicesController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val cache = DataCacheConnector
}
