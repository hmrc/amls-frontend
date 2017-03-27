package connectors

import config.{AmlsSessionCache, BusinessCustomerSessionCache}
import models.businesscustomer.ReviewDetails
import models.status.ConfirmationStatus
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

trait KeystoreConnector {

  private[connectors] def businessCustomerDataCache: SessionCache
  private[connectors] def amlsDataCache: SessionCache

  private val key = "BC_Business_Details"

  def optionalReviewDetails
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[Option[ReviewDetails]] =
    businessCustomerDataCache.fetchAndGetEntry[ReviewDetails](key)

  def reviewDetails
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[ReviewDetails] =
    businessCustomerDataCache.fetchAndGetEntry[ReviewDetails](key) flatMap {
      case Some(reviewDetails) =>
        Future.successful(reviewDetails)
      case None =>
        Future.failed {
          new NotFoundException("No review details found for Session")
        }
    }

  def confirmationStatus(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    amlsDataCache.fetchAndGetEntry[ConfirmationStatus](ConfirmationStatus.key) flatMap {
      case Some(s) => Future.successful(s)
      case _ => Future.successful(ConfirmationStatus(None))
    }

  def setConfirmationStatus(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    amlsDataCache.cache(ConfirmationStatus.key, ConfirmationStatus(Some(true))) flatMap {map =>Future.successful(map) }

  def resetConfirmation(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    amlsDataCache.cache(ConfirmationStatus.key, ConfirmationStatus(None)) flatMap { map => Future.successful(map) }

}

object KeystoreConnector extends KeystoreConnector {
  // $COVERAGE-OFF$
  override private[connectors] def businessCustomerDataCache: SessionCache = BusinessCustomerSessionCache
  override private[connectors] def amlsDataCache: SessionCache = AmlsSessionCache
}
