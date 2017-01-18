package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.status.SubmissionDecisionApproved
import models.tradingpremises._
import org.joda.time.LocalDate
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import services.StatusService
import utils.{FeatureToggle, RepeatingSection}
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
                  tp.agentPartnership,tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices, hasChanged = true, tp.lineId, tp.status, tp.endDate)
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
        Ok(views.html.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)),
          "summary.tradingpremises", controllers.tradingpremises.routes.WhereAreTradingPremisesController.saveDateOfChange(index)))
    }
  }

  def saveDateOfChange(index: Int) = Authorised.async {
    implicit authContext => implicit request =>

      implicit class FormExtensions(form: InvalidForm) {
        def withMessageFor(p: Path, message: String) = {
          InvalidForm(form.data, (form.errors filter (x => x._1 != p)) :+ (p, Seq(ValidationError(message))))
        }
      }

        getData[TradingPremises](index) flatMap { tradingPremises =>
          val extraFields = tradingPremises.yourTradingPremises.fold(Map[String, Seq[String]]()) { ytp =>
            Map("activityStartDate" -> Seq(ytp.startDate.toString("yyyy-MM-dd")))
          }

          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case form: InvalidForm =>

              val f = form.withMessageFor(Path \ "dateOfChange", "HAY U GUYZ!")

              Future.successful(BadRequest(views.html.date_of_change(f, "summary.tradingpremises", routes.WhereAreTradingPremisesController.saveDateOfChange(index))))
            case ValidForm(_, dateOfChange) =>
              updateDataStrict[TradingPremises](index) { tp =>
                tp.yourTradingPremises.fold(tp) { ytp =>
                  tp.copy(
                    yourTradingPremises = Some(ytp.copy(
                      tradingNameChangeDate = Some(dateOfChange),
                      tradingPremisesAddress = ytp.tradingPremisesAddress.copy(dateOfChange = Some(dateOfChange))
                      )))
                }
              } map { _ =>
                Redirect(routes.SummaryController.get())
              }
          }
        }
  }

  private def redirectToDateOfChange(tradingPremises: TradingPremises, premises: YourTradingPremises) =
  ApplicationConfig.release7 && {
    tradingPremises.yourTradingPremises.fold(false) { ytp =>
      ytp.tradingName != premises.tradingName || ytp.tradingPremisesAddress != premises.tradingPremisesAddress
    }
  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
