package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.AgentCompanyName
import models.tradingpremises.AgentCompanyName
import models.tradingpremises.AgentCompanyName.AgentCompanyName
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.tradingpremises.agent_company_name


import scala.concurrent.Future

trait AgentCompanyNameController extends BaseController {

  private[controllers] def dataCacheConnector: DataCacheConnector

  def get(index: Int ,edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[TradingPremises](TradingPremises.key) map {
        response =>
          val form: Form2[AgentCompanyName] = (for {
            tradingPremises <- response
            agent <- tradingPremises.agentCompanyName
          } yield Form2[AgentCompanyName](agent)).getOrElse(EmptyForm)
          Ok(agent_company_name(form, index, edit))
      }
  }

  def post(index: Int ,edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AgentCompanyName](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(agent_company_name(f, index,edit)))
        case ValidForm(_, data) =>
          for {
            tradingPremises <- dataCacheConnector.fetch[TradingPremises](TradingPremises.key)
            _ <- dataCacheConnector.save[TradingPremises](TradingPremises.key,
              tradingPremises.agentCompanyName(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SummaryController.get())
          }
      }
    }
  }
}

object AgentCompanyNameController extends AgentCompanyNameController {
  // $COVERAGE-OFF$
  override private[controllers] def dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
