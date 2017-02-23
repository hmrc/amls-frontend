package connectors

import config.BusinessCustomerSessionCache
import models.businesscustomer.ReviewDetails
import models.status.ConfirmationStatus
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

trait KeystoreConnector {

  private[connectors] def dataCache: SessionCache

  private val key = "BC_Business_Details"

  def optionalReviewDetails
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[Option[ReviewDetails]] =
    dataCache.fetchAndGetEntry[ReviewDetails](key)

  def reviewDetails
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[ReviewDetails] =
    dataCache.fetchAndGetEntry[ReviewDetails](key) flatMap {
      case Some(reviewDetails) =>
        Future.successful(reviewDetails)
      case None =>
        Future.failed {
          new NotFoundException("No review details found for Session")
        }
    }

  def confirmationStatus(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    dataCache.fetchAndGetEntry[ConfirmationStatus](ConfirmationStatus.key) flatMap {
      case Some(s) => Future.successful(s)
      case _ => Future.successful(ConfirmationStatus(None))
    }
}

object KeystoreConnector extends KeystoreConnector {
  // $COVERAGE-OFF$
  override private[connectors] def dataCache: SessionCache = BusinessCustomerSessionCache
}
