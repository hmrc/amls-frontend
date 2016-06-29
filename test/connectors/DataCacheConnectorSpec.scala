package connectors

import config.AmlsShortLivedCache
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.{LoggedInUser, AuthContext}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class DataCacheConnectorSpec
  extends PlaySpec
    with OneAppPerSuite
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience {

  case class Model(value: String)
  object Model {
    implicit val format = Json.format[Model]
  }

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]
    implicit val user = mock[LoggedInUser]
    val oid = "user_oid"
    val key = "key"

    when(authContext.user) thenReturn user
    when(user.oid) thenReturn oid
  }

  val emptyCache = CacheMap("", Map.empty)

  object DataCacheConnector extends DataCacheConnector {
    override val shortLivedCache: ShortLivedCache = mock[ShortLivedCache]
  }

  "DataCacheConnector" must {

    "use the correct session cache for Amls" in {
      connectors.DataCacheConnector.shortLivedCache mustBe AmlsShortLivedCache
    }

    "save data to save4later" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.shortLivedCache.cache(eqTo(oid), eqTo(key), eqTo(model))(any(), any())
      } thenReturn Future.successful(emptyCache)

      val result = DataCacheConnector.save(key, model)

      whenReady(result) {
        result =>
          result must be (emptyCache)
      }
    }

    "fetch saved data from save4later" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.shortLivedCache.fetchAndGetEntry[Model](eqTo(oid), eqTo(key))(any(), any())
      } thenReturn Future.successful(Some(model))

      val result = DataCacheConnector.fetch[Model](key)

      whenReady (result) {
        result =>
          result must be (Some(model))
      }
    }

    "fetch all data from save4later" in new Fixture {

      val model = Model("data")

      when {
        DataCacheConnector.shortLivedCache.fetch(eqTo(oid))(any())
      } thenReturn Future.successful(Some(emptyCache))

      val result = DataCacheConnector.fetchAll

      whenReady(result) {
        result =>
          result must be (Some(emptyCache))
      }
    }
  }
}
