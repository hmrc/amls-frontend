import connectors.AmlsConnector
import models.SubscriptionRequest
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import org.specs2.mock.Mockito
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http.HeaderCarrier

class SubscriptionIT extends PlaySpec
    with ScalaFutures
    with OneServerPerSuite
    with Mockito {

  "Subscription Integeration" when {
    "passed a minimal request" should {
      "return a successful response" in {
        implicit val hc = HeaderCarrier()
        implicit val user =  AuthContext(
          user = LoggedInUser(
            userId = "userId",
            loggedInAt = None,
            previouslyLoggedInAt = None,
            governmentGatewayToken = None,
            confidenceLevel = ConfidenceLevel.L50
          ),
          principal = Principal(
            name = None,
            accounts = Accounts(vat = Some(VatAccount("link",Vrn("1234567890"))))
          ),
          attorney = None
        )

        val data = SubscriptionRequest(Some("Reference"),None)
        val result = AmlsConnector.subscribe(data, "XA0001234567890")
        whenReady(result) { result =>
          result.status must be(OK)
        }
      }
    }

  }
}
