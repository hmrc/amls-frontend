package connectors

import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AuthenticatorConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  trait TestFixture {

    val http = mock[HttpPost]
    val serviceConfig = mock[ServicesConfig]

    implicit val hc = HeaderCarrier()

    val featureToggleSetting: Boolean

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure("Test.microservice.services.feature-toggle.refresh-profile" -> featureToggleSetting)
      .overrides(bind[HttpPost].to(http))
      .build()

    lazy val connector = app.injector.instanceOf(classOf[AuthenticatorConnector])

    when(serviceConfig.baseUrl(any())) thenReturn "authenticator.service"
  }

  "The Authenticator connector" must {

    "connect to the authenticator service to refresh the auth profile" in new TestFixture {

      val featureToggleSetting = true

      when(http.POSTEmpty[HttpResponse](any())(any(), any())) thenReturn Future.successful(HttpResponse(200))

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http).POSTEmpty(eqTo("http://localhost:9905/authenticator/refresh-profile"))(any(), any())

    }

    "return a default successful result when the feature is toggled off" in new TestFixture {

      val featureToggleSetting = false

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http, never).POSTEmpty(any())(any(), any())
    }

  }

}
