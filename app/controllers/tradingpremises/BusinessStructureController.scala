package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import play.api.libs.json.Json
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

trait BusinessStructureController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        response =>
          val form = (for {
            tp <- response
            services <- tp.businessStructure
          } yield Form2[BusinessStructure](services)).getOrElse(EmptyForm)

          Ok(views.html.tradingpremises.business_structure(form, index, edit))
      }
  }

  def redirectToPage(data: BusinessStructure) = {
    data match {
      case SoleProprietor => Redirect(routes.SummaryController.get())
      case LimitedLiabilityPartnership => Redirect(routes.SummaryController.get())
      case Partnership => Redirect(routes.SummaryController.get())
      case IncorporatedBody => Redirect(routes.SummaryController.get())
      case UnincorporatedBody => Redirect(routes.SummaryController.get())
    }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[BusinessStructure](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.business_structure(f, index, edit)))
        case ValidForm(_, data) =>
          for {
            _ <- updateData[TradingPremises](index) {
              case Some(tp) => Some(tp.businessStructure(data))
              case _ => Some(TradingPremises(businessStructure = Some(data)))
            }
          } yield {
              redirectToPage(data)
          }
      }
  }
}

object BusinessStructureController extends BusinessStructureController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
