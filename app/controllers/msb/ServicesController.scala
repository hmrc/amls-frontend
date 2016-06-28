package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness._
import play.api.mvc.{Result, Call}
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

  private def editRouting(services: Set[MsbService], msb: MoneyServiceBusiness): Result =
    services match {
      case s if s contains TransmittingMoney =>
        mtRouting(services, msb)
      case s if s contains CurrencyExchange =>
        ceRouting(msb)
      case _ =>
        Redirect(routes.SummaryController.get())
    }

  private def mtRouting(services: Set[MsbService], msb: MoneyServiceBusiness): Result =
    if (msb.businessAppliedForPSRNumber.isDefined) {
      editRouting(services - TransmittingMoney, msb)
    } else {
      Redirect(routes.BusinessAppliedForPSRNumberController.get(true))
    }

  private def ceRouting(msb: MoneyServiceBusiness): Result =
    if (msb.ceTransactionsInNext12Months.isDefined) {
      Redirect(routes.SummaryController.get())
    } else {
      Redirect(routes.CETransactionsInNext12MonthsController.get(true))
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
          } yield edit match {
            case false =>
              Redirect(routes.ExpectedThroughputController.get())
            case true =>
              editRouting(data.services, msb)
          }
      }
  }
}

object ServicesController extends ServicesController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override val cache = DataCacheConnector
}
