package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import models.estateagentbusiness.EstateAgentBusiness
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

trait BankAccountAddController extends RepeatingSection with BaseController {
  def get() = Authorised.async {
    implicit authContext => implicit request =>
      addData[BankDetails](None).map { idx =>
        Redirect(routes.BankAccountTypeController.get(idx, false))
      }
  }
}

object BankAccountAddController extends BankAccountAddController {
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}