package controllers.bankdetails

import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BankAccountUtilController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def getBankDetails(implicit user: AuthContext, hc: HeaderCarrier): Future[Seq[BankDetails]] = {
    dataCacheConnector.fetchDataShortLivedCache[Seq[BankDetails]](BankDetails.key) map {
      _.fold(Seq.empty[BankDetails]) {
        identity
      }
    }
  }

  def getBankDetails(index: Int)(implicit user: AuthContext, hc: HeaderCarrier): Future[Option[BankDetails]] = {
    getBankDetails map {
      case accounts if index > 0 && index <= accounts.length + 1 => accounts lift (index - 1)
      case _ => None
    }
  }

  protected def updateBankDetails
  (index: Int)
  (fn: Option[BankDetails] => Option[BankDetails])
  (implicit user: AuthContext, hc: HeaderCarrier): Future[_] =
    getBankDetails map {
      accounts => {

        putBankDetails(accounts.patch(index - 1, fn(accounts.lift(index - 1)).toSeq, 1))
      }
    }

  protected def putBankDetails(accounts: Seq[BankDetails])
  (implicit user: AuthContext, hc: HeaderCarrier): Future[_] =
    dataCacheConnector.saveDataShortLivedCache[Seq[BankDetails]](BankDetails.key, accounts)
}

