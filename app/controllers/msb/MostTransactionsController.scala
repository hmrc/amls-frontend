package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{MoneyServiceBusiness, MostTransactions}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait MostTransactionsController extends BaseController {

  def cache: DataCacheConnector

  def get(edit: Boolean) = Authorised.async {
    implicit authContext => implicit request =>

      cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
        response =>

          val form = (for {
            msb <- response
            transactions <- msb.mostTransactions
          } yield Form2[MostTransactions](transactions)).getOrElse(EmptyForm)

          Ok(views.html.msb.most_transactions(form, edit))
      }
  }

  def post(edit: Boolean) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[MostTransactions](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.msb.most_transactions(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- cache.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- cache.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.mostTransactions(data)
            )
          } yield edit match {
            case false =>
              // TODO: Linked transactions page
              Redirect(routes.ServicesController.get())
            case true =>
              Redirect(routes.SummaryController.get())
          }
      }
  }
}

object MostTransactionsController extends MostTransactionsController {
  override val cache: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}
