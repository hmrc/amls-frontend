package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.DateOfChange
import models.status.SubmissionDecisionApproved
import models.tradingpremises.{MsbServices, TradingPremises}
import org.joda.time.LocalDate
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{DateOfChangeHelper, RepeatingSection}

import scala.concurrent.Future

trait MSBServicesController extends RepeatingSection with BaseController with DateOfChangeHelper with FormHelpers {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(tp) => {
          val form = tp.msbServices match {
            case Some(service) => Form2[MsbServices](service)
            case None => EmptyForm
          }
          Ok(views.html.tradingpremises.msb_services(form, index, edit))
        }
        case None => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MsbServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.msb_services(f, index, edit)))
        case ValidForm(_, data) => {
          for {
            tradingPremises <- getData[TradingPremises](index)
            _ <- updateDataStrict[TradingPremises](index) { tp =>
              tp.msbServices(data)
            }
            status <- statusService.getStatus
          } yield status match {
            case SubmissionDecisionApproved if redirectToDateOfChange(tradingPremises, data) && edit && tradingPremises.lineId.isDefined =>
              Redirect(routes.WhatDoesYourBusinessDoController.dateOfChange(index))
            case _ => edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.PremisesRegisteredController.get(index))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  def redirectToDateOfChange(tradingPremises: TradingPremises, msbServices: MsbServices) =
    ApplicationConfig.release7 && !tradingPremises.msbServices.contains(msbServices)
}

object MSBServicesController extends MSBServicesController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
