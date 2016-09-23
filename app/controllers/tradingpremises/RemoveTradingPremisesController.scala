package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, Form2, InvalidForm, EmptyForm}
import models.tradingpremises.{ActivityEndDate, TradingPremises}
import utils.{StatusConstants, RepeatingSection}
import views.html.tradingpremises.remove_trading_premises

import scala.concurrent.Future

trait RemoveTradingPremisesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, complete: Boolean = false, tradingName: String, showDateField: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.tradingpremises.remove_trading_premises(EmptyForm, index, complete, tradingName, showDateField)))
  }

  def remove(index: Int, complete: Boolean = false, tradingName: String, showDateField: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      showDateField match {
        case true =>
          Form2[ActivityEndDate](request.body) match {
            case f: InvalidForm =>
              Future.successful(BadRequest(remove_trading_premises(f, index, complete, tradingName, showDateField)))
            case ValidForm(_, data) =>
              for {
                result <- updateDataStrict[TradingPremises](index) { tp =>
                  tp.copy(status = Some(StatusConstants.Deleted), endDate = Some(data))
                }
              } yield Redirect(routes.SummaryController.get(complete))
          }
        case false => removeDataStrict[TradingPremises](index) map { _ =>
          Redirect(routes.SummaryController.get(complete))
        }
      }
  }
}

object RemoveTradingPremisesController extends RemoveTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
