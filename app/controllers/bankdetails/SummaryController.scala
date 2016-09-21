package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import models.estateagentbusiness.EstateAgentBusiness
import utils.RepeatingSection

import scala.concurrent.Future

trait SummaryController extends RepeatingSection with BaseController  {

  val dataCacheConnector: DataCacheConnector

  def get(complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Seq[BankDetails]](BankDetails.key) map {
        case Some(data) => Ok(views.html.bankdetails.summary(data, complete, hasBankAccount(data)))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  private def hasBankAccount(bankDetails: Seq[BankDetails]): Boolean = {
    bankDetails.exists(_.bankAccount.isDefined)
  }

  def remove(index:Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
        removeDataStrict[BankDetails](index) map {_ =>
          Redirect(routes.SummaryController.get(complete))
        }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
