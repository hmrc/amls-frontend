package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

trait LargestAmountsController extends BaseController {

  def cache: DataCacheConnector

  def get(edit: Boolean) = Authorised.async {
    implicit authContext => implicit request =>

      cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>

//          val form = (for {
//            msb <- response
//            branches <- msb.branchesOrAgents
//          } yield Form2[MostTransactions](branches)).getOrElse(EmptyForm)
//
//          Ok(views.html.msb.branches_or_agents(form, edit))
          Ok("")
      }
  }
}

object LargestAmountsController extends LargestAmountsController {
  override val cache: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
