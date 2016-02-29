import connectors.AmlsConnector
import models.SubscriptionRequest
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import org.specs2.mock.Mockito
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.it.{ExternalService, MicroServiceEmbeddedServer}

class IntegrationServer(override val testName : String) extends MicroServiceEmbeddedServer {
  override protected val externalServices: Seq[ExternalService] =

    Seq("amls",
      "amls-stub").map(ExternalService.runFromJar(_))
}

class SubscriptionIT extends PlaySpec
with Mockito
with ScalaFutures
with OneServerPerSuite
with IntegrationPatience
with BeforeAndAfterEach{
  val server = new IntegrationServer("TestName")

  override def beforeEach = {server.start()}
  //override def afterEach = {server.stop()}

  "Subscription Integeration" when {
    "passed a minimal request" should {
      "return a successful response" in {
        implicit val hc = HeaderCarrier()
        implicit val user =  mock[AuthContext]

        val testConnector = new AmlsConnector {
          override val serviceURL : String = server.externalResource("amls","")
        }

        val data = SubscriptionRequest(Some("Reference"),None)
        val result = testConnector.subscribe(data, "XA0001234567890")

        whenReady(result) { result =>
          result.status must be(OK)
        }
      }
    }
  }
}
