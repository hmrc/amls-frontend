package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.status._
import models.tradingpremises.{ActivityEndDate, TradingPremises}
import services.{AuthEnrolmentsService, StatusService}
import utils.{RepeatingSection, StatusConstants}
import views.html.tradingpremises.remove_trading_premises

import scala.concurrent.Future

trait RemoveTradingPremisesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  private[controllers] def authEnrolmentsService: AuthEnrolmentsService

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        tp <- getData[TradingPremises](index)
        status <- statusService.getStatus
      } yield (tp, status) match {

        case (Some(_), SubmissionDecisionApproved | ReadyForRenewal(_)) =>
          Ok(views.html.tradingpremises.remove_trading_premises(EmptyForm, index, complete,
            tp.yourTradingPremises.fold("")(_.tradingName), showDateField = tp.lineId.isDefined))

        case (Some(_), _) => Ok(views.html.tradingpremises.remove_trading_premises(EmptyForm, index, complete,
          tp.yourTradingPremises.fold("")(_.tradingName), showDateField = false))

        case _ => NotFound(notFoundView)
      }
  }

  def remove(index: Int, complete: Boolean = false, tradingName: String) = Authorised.async {
    implicit authContext => implicit request =>

      statusService.getStatus flatMap {
        case NotCompleted | SubmissionReady => removeDataStrict[TradingPremises](index) map { _ =>
          Redirect(routes.SummaryController.get(complete))
        }
        case SubmissionReadyForReview => for {
          _ <- updateDataStrict[TradingPremises](index) { tp =>
            tp.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
          }
        } yield Redirect(routes.SummaryController.get(complete))
        case _ =>
          getData[TradingPremises](index) flatMap { premises =>
            premises.lineId match {
              case Some(_) =>
                val extraFields = Map(
                  "premisesStartDate" -> Seq(premises.get.yourTradingPremises.get.startDate.get.toString("yyyy-MM-dd"))
                )

                Form2[ActivityEndDate](request.body.asFormUrlEncoded.get ++ extraFields) match {
                  case f: InvalidForm =>
                    Future.successful(BadRequest(remove_trading_premises(f, index, complete, tradingName, true)))
                  case ValidForm(_, data) => {
                    for {
                      _ <- updateDataStrict[TradingPremises](index) { tp =>
                        tp.copy(status = Some(StatusConstants.Deleted), endDate = Some(data), hasChanged = true)
                      }
                    } yield Redirect(routes.SummaryController.get(complete))
                  }
                }

              case _ =>
                updateDataStrict[TradingPremises](index) {
                  _.copy(status = Some(StatusConstants.Deleted), hasChanged = true)
                } map { _ =>
                  Redirect(routes.SummaryController.get(complete))
                }
            }
          }
      }
  }
}

object RemoveTradingPremisesController extends RemoveTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService
  override private[controllers] val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService

}
