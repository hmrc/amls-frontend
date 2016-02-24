package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import models.estateagentbusiness.EstateAgentBusiness

import scala.concurrent.Future

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchDataShortLivedCache[Seq[BankDetails]](BankDetails.key) map {
        case Some(data) => Ok(views.html.bank_details_summary(data))
        case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object SummaryController extends SummaryController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
