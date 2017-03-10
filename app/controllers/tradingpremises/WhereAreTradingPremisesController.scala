package controllers.tradingpremises

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, FormHelpers, InvalidForm, ValidForm}
import models.DateOfChange
import models.status.SubmissionDecisionApproved
import models.tradingpremises._
import org.joda.time.LocalDate
import play.api.i18n.Messages
import services.StatusService
import utils.{DateOfChangeHelper, FeatureToggle, RepeatingSection}
import views.html.tradingpremises._

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends RepeatingSection with BaseController with DateOfChangeHelper with FormHelpers {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(TradingPremises(_, Some(data), _, _, _, _, _, _, _, _, _, _, _, _)) =>
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
              val updatedYtp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None)(x =>
                Some(ytp.copy(startDate = x.startDate, isResidential = x.isResidential)))
              TradingPremises(tp.registeringAgentPremises,
                updatedYtp, tp.businessStructure, tp.agentName, tp.agentCompanyDetails,
                tp.agentPartnership, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices, hasChanged = true, tp.lineId, tp.status, tp.endDate)
            }
            status <- statusService.getStatus
          } yield status match {
            case SubmissionDecisionApproved if redirectToDateOfChange(tradingPremises, ytp) && edit =>
              Redirect(routes.WhereAreTradingPremisesController.dateOfChange(index))
            case _ => edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.ActivityStartDateController.get(index, edit))
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
      getData[TradingPremises](index) flatMap { tradingPremises =>
        Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ startDateFormFields(tradingPremises.startDate)) match {
          case form: InvalidForm =>
            Future.successful(BadRequest(
              views.html.date_of_change(
                form.withMessageFor(DateOfChange.errorPath, tradingPremises.startDateValidationMessage),
                "summary.tradingpremises", routes.WhereAreTradingPremisesController.saveDateOfChange(index))))
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

  private def redirectToDateOfChange(tradingPremises: Option[TradingPremises], premises: YourTradingPremises) =
    ApplicationConfig.release7 && (for {
      tp <- tradingPremises
      ytp <- tp.yourTradingPremises
    } yield (ytp.tradingName != premises.tradingName || ytp.tradingPremisesAddress != premises.tradingPremisesAddress) && tp.lineId.isDefined
      ).getOrElse(false)

}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
