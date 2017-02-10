package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.hvd.routes
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.hvd.{Alcohol, Hvd, Products, Tobacco}
import models.status.SubmissionDecisionApproved
import models.tradingpremises.TradingPremises
import services.{AuthEnrolmentsService, StatusService}
import utils.RepeatingSection
import views.html.tradingpremises.remove_agent_premises_reasons

import scala.concurrent.Future


trait RemoveAgentPremisesReasonsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector
  private[controllers] def statusService: StatusService
  //private[controllers] def authEnrolmentsService: AuthEnrolmentsService

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        tp <- getData[TradingPremises](index)
      } yield (tp) match {
        case (Some(tradingPremises)) => {
          Ok(views.html.tradingpremises.remove_agent_premises_reasons(EmptyForm, index, complete,
            tp.yourTradingPremises.fold("")(_.tradingName)))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Boolean = false, complete: Boolean = false, tradingName: String) =
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[Products](request.body) match {
          case f: InvalidForm =>
            ??? //Future.successful(BadRequest(products(f, edit)))
          case ValidForm(_, data) => {
            for {
              hvd <- dataCacheConnector.fetch[Hvd](Hvd.key)
              status <- statusService.getStatus
              _ <- dataCacheConnector.save[Hvd](Hvd.key,
                hvd.products(data)
              )
            } yield status match {
              case SubmissionDecisionApproved =>
                ???
              case _ => ???
              }
            }
          }
        }
    }
}



object RemoveAgentPremisesReasonsController extends RemoveAgentPremisesReasonsController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService
  //override private[controllers] val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService

}

