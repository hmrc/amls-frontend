import connectors.AmlsConnector
import models.SubscriptionRequest
import models.estateagentbusiness._
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.{OneServerPerTest, OneServerPerSuite, PlaySpec}
import org.specs2.mock.Mockito
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domain.{TaxIdentifier, SaUtr}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.it.{ExternalService, MicroServiceEmbeddedServer}
import uk.gov.hmrc.test.it.{AuthorisationHeader, FrontendCookieHelper}



class IntegrationServer(override val testName : String) extends MicroServiceEmbeddedServer {
  override protected val externalServices: Seq[ExternalService] =

    Seq("amls-stub", "auth").map(ExternalService.runFromJar(_)) ++ Seq("amls").map(ExternalService.runFromSource(_))
}

class SubscriptionIT extends PlaySpec
with Mockito
with ScalaFutures
with OneServerPerTest
with IntegrationPatience
with BeforeAndAfterEach with FrontendCookieHelper{
  val server = new IntegrationServer("TestName")
  val utr = "1234567890"
  override def beforeEach = {server.start()}

  def authResource(path: String) = server.externalResource("auth", path)

  "Subscription Integeration" when {
    "passed a minimal request" should {
      "return a successful response" in {

        lazy val ggAuthHeader = createGGAuthorisationHeader(SaUtr(utr))

        lazy val ggAuthorisationHeader = AuthorisationHeader.forGovernmentGateway(authResource, s"utr-${utr}")

        def createGGAuthorisationHeader(ids: TaxIdentifier*): (String, String) = ggAuthorisationHeader.create(ids.toList).futureValue

        lazy val cookie = cookieFor(ggAuthorisationHeader.createBearerToken(List(SaUtr(utr))).futureValue).futureValue

        def cookieForUtr(utr: SaUtr) = cookieFor(ggAuthorisationHeader.createBearerToken(List(utr)).futureValue)

        implicit val hc = HeaderCarrier(authorization = Some(Authorization(ggAuthorisationHeader.gatewayId)),
          extraHeaders = Seq(cookie))
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
//    "Passed a request including an EAB Section" should {
//      "return a successful response" in  {
//        val request = FakeRequest()
//          .withHeaders(CONTENT_TYPE -> "application/json")
//        val data = SubscriptionRequest(None,
//          Some(EstateAgentBusiness(
//            Some(Services(Set(Auction, BusinessTransfer))),
//            Some(OmbudsmanServices),
//            Some(ProfessionalBodyNo),
//            Some(PenalisedUnderEstateAgentsActNo)
//          )))
//        val result = AmlsConnector.subscribe(data, "XA0001234567890")
//        whenReady(result) { result =>
//          result.status must be(OK)
//        }
//
//      }
//    }

    /*    "Passed an invalid request" should {
          "return a failed response" in {
            val data = SubscriptionRequest(Some("a"), None)

            val result = AmlsConnector.subscribe(data, "XA0001234567890")
            whenReady(result) { result =>
              result.status must be(BAD_REQUEST)
            }
          }
        }*/
  }
}
