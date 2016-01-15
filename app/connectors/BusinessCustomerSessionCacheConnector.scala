package connectors

import config.BusinessCustomerSessionCache
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BusinessCustomerSessionCacheConnector {

  def businessCustomerSessionCache: SessionCache

  private val BCSourceId = "BC_Business_Details"

  private def fetchAndGetData[T](key: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    businessCustomerSessionCache.fetchAndGetEntry[T](key)
  }

  def getReviewBusinessDetails[T](implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    fetchAndGetData[T](BCSourceId)
  }
}

object BusinessCustomerSessionCacheConnector extends BusinessCustomerSessionCacheConnector {
  override def businessCustomerSessionCache = BusinessCustomerSessionCache
}
