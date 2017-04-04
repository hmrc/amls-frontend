package controllers.renewal

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.renewal.{MostTransactions, Renewal}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper

import scala.concurrent.Future

@Singleton
class MostTransactionsController @Inject()(val authConnector: AuthConnector,
                                           val cache: DataCacheConnector) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        cache.fetch[Renewal](Renewal.key) map {
          response =>
            val form = (for {
              msb <- response
              transactions <- msb.mostTransactions
            } yield Form2[MostTransactions](transactions)).getOrElse(EmptyForm)
            Ok(views.html.renewal.most_transactions(form, edit))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[MostTransactions](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.renewal.most_transactions(f, edit)))
          case ValidForm(_, data) =>
            for {
              renewal <- cache.fetch[Renewal](Renewal.key)
              _ <- cache.save[Renewal](Renewal.key, renewal.mostTransactions(data))
            } yield Redirect(routes.SummaryController.get())
        }
  }
}
