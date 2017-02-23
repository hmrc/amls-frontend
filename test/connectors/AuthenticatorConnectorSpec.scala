package connectors

import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.mockito.internal.verification.Times
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.bind
import play.inject.guice.GuiceInjectorBuilder
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AuthenticatorConnectorSpec extends PlaySpec with ScalaFutures with MockitoSugar {

  trait TestFixture {

    val http = mock[HttpPost]
    val serviceConfig = mock[ServicesConfig]

    val defaultBuilder = new GuiceInjectorBuilder()
      .bindings(bind[HttpPost].to(http))
      .bindings(bind[ServicesConfig].to(serviceConfig))

    val builder = defaultBuilder

    lazy val injector = builder.build()

    lazy val connector = injector.instanceOf(classOf[AuthenticatorConnector])

    implicit val hc = HeaderCarrier()

    when(serviceConfig.baseUrl(any())) thenReturn "authenticator.service"
  }

  "The Authenticator connector" must {

    "connect to the authenticator service to refresh the auth profile" in new TestFixture {

      when(serviceConfig.getConfBool(any(), any())) thenReturn true

      when(http.POSTEmpty[HttpResponse](any())(any(), any())) thenReturn Future.successful(HttpResponse(200))

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http).POSTEmpty(eqTo("authenticator.service/authenticator/refresh-profile"))(any(), any())

    }

    "return a default successful result when the feature is toggled off" in new TestFixture {

      when(serviceConfig.getConfBool(any(), any())) thenReturn false

      val result = Await.result(connector.refreshProfile, 5 seconds)

      result.status must be(200)

      verify(http, never).POSTEmpty(any())(any(), any())
    }

  }

}
