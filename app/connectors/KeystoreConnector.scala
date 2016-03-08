package connectors

import config.BusinessCustomerSessionCache
import models.businesscustomer.ReviewDetails
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.{NotFoundException, HeaderCarrier}

import scala.concurrent.{Future, ExecutionContext}

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
}

object KeystoreConnector extends KeystoreConnector {
  override private[connectors] def dataCache: SessionCache = BusinessCustomerSessionCache
}
