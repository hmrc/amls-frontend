package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionStatus}
import models.tradingpremises.{MsbServices, TradingPremises}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{DateOfChangeHelper, RepeatingSection}

import scala.concurrent.Future

trait MSBServicesController extends RepeatingSection with BaseController with DateOfChangeHelper with FormHelpers {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false, changed: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(tp) => {
          val form = tp.msbServices match {
            case Some(service) => Form2[MsbServices](service)
            case None => EmptyForm
          }
          Ok(views.html.tradingpremises.msb_services(form, index, edit, changed))
        }
        case None => NotFound(notFoundView)
      }
  }

  private def redirectBasedOnStatus(status: SubmissionStatus,
                            tradingPremises: Option[TradingPremises],
                            data:MsbServices,
                            edit: Boolean,
                            changed:Boolean,
                            index:Int) = {
    status match {
      case SubmissionDecisionApproved | ReadyForRenewal(_) if this.redirectToDateOfChange(tradingPremises, data, changed)
        && edit && tradingPremises.lineId.isDefined =>
        Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
      case _ => edit match {
        case true => Redirect(routes.SummaryController.getIndividual(index))
        case false => Redirect(routes.PremisesRegisteredController.get(index))
      }
    }
  }

  def post(index: Int, edit: Boolean = false, changed: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.msb_services(f, index, edit, changed)))
        case ValidForm(_, data) => {
          for {
            tradingPremises <- getData[TradingPremises](index)
            _ <- updateDataStrict[TradingPremises](index) { tp =>
              tp.msbServices(data)
            }
            status <- statusService.getStatus
          } yield redirectBasedOnStatus(status, tradingPremises, data, edit, changed, index)
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  def redirectToDateOfChange(tradingPremises: Option[TradingPremises], msbServices: MsbServices, force: Boolean = false) =
    ApplicationConfig.release7 && (!tradingPremises.get.msbServices.contains(msbServices) || force)
}

object MSBServicesController extends MSBServicesController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
