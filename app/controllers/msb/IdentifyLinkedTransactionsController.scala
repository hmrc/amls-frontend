package controllers.msb

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.{IdentifyLinkedTransactions, MoneyServiceBusiness}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.msb.identify_linked_transactions

import scala.concurrent.Future

trait IdentifyLinkedTransactionsController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit:Boolean = false) = Authorised.async {
   implicit authContext => implicit request =>
     dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key) map {
       response =>
         val form: Form2[IdentifyLinkedTransactions] = (for {
           msb <- response
           accountant <- msb.identifyLinkedTransactions
         } yield Form2[IdentifyLinkedTransactions](accountant)).getOrElse(EmptyForm)
         Ok(identify_linked_transactions(form, edit))
     }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[IdentifyLinkedTransactions](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(identify_linked_transactions(f, edit)))
        case ValidForm(_, data) =>
          for {
            msb <- dataCacheConnector.fetch[MoneyServiceBusiness](MoneyServiceBusiness.key)
            _ <- dataCacheConnector.save[MoneyServiceBusiness](MoneyServiceBusiness.key,
              msb.identifyLinkedTransactions(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.SummaryController.get())
          }
      }
    }
  }
}

object IdentifyLinkedTransactionsController extends IdentifyLinkedTransactionsController {
  // $COVERAGE-OFF$
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
