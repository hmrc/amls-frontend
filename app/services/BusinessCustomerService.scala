package services

import connectors.{BusinessCustomerDataCacheConnector, DataCacheConnector}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait BusinessCustomerService {
  def dataCacheConnector: DataCacheConnector

  val bcSourceId: String = "BC_Business_Details"

  def getReviewBusinessDetails[T](implicit hc: HeaderCarrier, formats: json.Format[T]) : Future[T] = {
    dataCacheConnector.fetchAndGetData[T](bcSourceId) flatMap {
      case Some(data) => Future.successful(data)
      case None => throw new RuntimeException("No Review Details Found")
    }
  }
}

object BusinessCustomerService extends BusinessCustomerService {
  val dataCacheConnector: DataCacheConnector = BusinessCustomerDataCacheConnector
}
