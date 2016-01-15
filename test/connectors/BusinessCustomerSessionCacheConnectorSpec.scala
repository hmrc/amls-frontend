package connectors

import config.BusinessCustomerSessionCache
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class BusinessCustomerSessionCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  val mockBusinessCustomerSessionCache = mock[SessionCache]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  case class ModelForString(name: String)

  object ModelForString {
    implicit val formats = Json.format[ModelForString]
  }

  //This is just to improve test coverage
  object TestBusinessCustomerSessionCacheConnector extends BusinessCustomerSessionCacheConnector {
    override def businessCustomerSessionCache: SessionCache = mockBusinessCustomerSessionCache
  }

  "BusinessCustomerSessionCacheConnector" must {
    "use the correct cache " in {
      TestBusinessCustomerSessionCacheConnector.businessCustomerSessionCache mustBe mockBusinessCustomerSessionCache
    }

    "get the Data" in {
      val dummyModelForString: ModelForString = ModelForString("dummy")
      when(mockBusinessCustomerSessionCache.fetchAndGetEntry[ModelForString] (any()) (any(), any())).thenReturn(Future.successful(Some(dummyModelForString)))
      val result = TestBusinessCustomerSessionCacheConnector.getReviewBusinessDetails[ModelForString]
      whenReady(result) { dtl =>
        dtl mustBe Some(dummyModelForString)
      }
    }
  }
}
