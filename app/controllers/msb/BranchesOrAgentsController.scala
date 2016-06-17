package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{BranchesOrAgents, MoneyServiceBusiness}
import play.api.data.mapping.Write
import play.api.data.mapping.forms.UrlFormEncoded
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait BranchesOrAgentsController extends BaseController {

  def cache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>

          val form = (for {
            msb <- response
            branches <- msb.branchesOrAgents
          } yield Form2[BranchesOrAgents](branches)).getOrElse(EmptyForm)

          Ok(views.html.msb.branches_or_agents(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[BranchesOrAgents](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.branches_or_agents(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.branchesOrAgents(data)
            )
          } yield edit match {
            case false =>
              Redirect(routes.IdentifyLinkedTransactionsController.get())
            case true =>
              Redirect(routes.SummaryController.get())
          }
      }
  }
}

object BranchesOrAgentsController extends BranchesOrAgentsController {
  override val cache: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
