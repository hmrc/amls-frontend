package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.aboutthebusiness.routes
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOffice}
import models.status.SubmissionDecisionApproved
import models.tradingpremises._
import org.joda.time.LocalDate
import play.api.Logger
import services.StatusService
import utils.{FeatureToggle, RepeatingSection}
import views.html.include.date_of_change
import views.html.tradingpremises._

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(TradingPremises(_,Some(data), _,_,_,_,_,_,_,_,_,_)) =>
          Ok(where_are_trading_premises(Form2[YourTradingPremises](data), edit, index))
        case Some(_) =>
          Ok(where_are_trading_premises(EmptyForm, edit, index))
        case _ =>
          NotFound(notFoundView)
      }
  }

  // TODO: Consider if this can be refactored
  // scalastyle:off cyclomatic.complexity
  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[YourTradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(where_are_trading_premises(f, edit, index)))
        case ValidForm(_, ytp) => {
          for {
            tradingPremises <- getData[TradingPremises](index)
            _ <- updateDataStrict[TradingPremises](index) { tp =>
                TradingPremises(tp.registeringAgentPremises,
                  Some(ytp), tp.businessStructure,tp.agentName,tp.agentCompanyName,
                  tp.agentPartnership,tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices, true, tp.lineId, tp.status, tp.endDate)
            }
            status <- statusService.getStatus
          } yield status match {
            case SubmissionDecisionApproved if redirectToDateOfChange(tradingPremises.get, ytp) =>
              Redirect(routes.WhereAreTradingPremisesController.dateOfChange(index))
            case _ => edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.WhatDoesYourBusinessDoController.get(index, edit))
            }
          }

        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  def dateOfChange(index: Int) = FeatureToggle(ApplicationConfig.release7) {
    Authorised {
      implicit authContext => implicit request =>
        Ok(views.html.include.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)),
          "summary.tradingpremises", controllers.tradingpremises.routes.WhereAreTradingPremisesController.saveDateOfChange(index)))
    }
  }

  def saveDateOfChange(index: Int) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) flatMap { aboutTheBusiness =>
          val extraFields: Map[String, Seq[String]] = aboutTheBusiness.get.activityStartDate match {
            case Some(date) => Map("activityStartDate" -> Seq(date.startDate.toString("yyyy-MM-dd")))
            case None => Map()
          }
          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case form: InvalidForm =>
              Future.successful(BadRequest(date_of_change(form, "summary.tradingpremises", routes.WhereAreTradingPremisesController.saveDateOfChange(index))))
            case ValidForm(_, dateOfChange) =>
              for {
                tradingPremises <- dataCacheConnector.fetch[TradingPremises](TradingPremises.key)
                _ <- dataCacheConnector.save[TradingPremises](TradingPremises.key,
                  tradingPremises.yourTradingPremises(tradingPremises.yourTradingPremises.get.copy(dateOfChange = Some(dateOfChange))))
              } yield Redirect(routes.SummaryController.get())
          }
        }
  }

  private def redirectToDateOfChange(tradingPremises: TradingPremises, premises: YourTradingPremises) =
    ApplicationConfig.release7 && !tradingPremises.yourTradingPremises.contains(premises)
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
