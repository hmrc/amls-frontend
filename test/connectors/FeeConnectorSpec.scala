package connectors

import models.ResponseType.SubscriptionResponseType
import models._
import org.joda.time.{DateTimeZone, DateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import uk.gov.hmrc.domain.{CtUtr, SaUtr, Org}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class FeeConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object FeeConnector extends FeeConnector {
    override private[connectors] val httpPost: HttpPost = mock[HttpPost]
    override private[connectors] val url: String = "amls/payment"
    override private[connectors] val httpGet: HttpGet = mock[HttpGet]
  }

  val safeId = "SAFEID"
  val amlsRegistrationNumber = "AMLSREGNO"

  implicit val hc = HeaderCarrier()
  implicit val ac = AuthContext(
    LoggedInUser(
      "UserName",
      None,
      None,
      None,
      CredentialStrength.Weak,
      ConfidenceLevel.L50),
    Principal(
      None,
      Accounts(org = Some(OrgAccount("Link", Org("TestOrgRef"))))),
    None,
    None,
    None)

  "FeeConnector" must {
    val amlsRegistrationNumber = "XAML00000567890"
    val feeResponse = FeeResponse(SubscriptionResponseType, amlsRegistrationNumber
      , 150.00, Some(100.0), 300.0, 550.0, Some("XA353523452345"), None,
      new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))

    "successfully receive feeResponse" in {

      when {
        FeeConnector.httpGet.GET[FeeResponse](eqTo(s"${FeeConnector.url}/org/TestOrgRef/$amlsRegistrationNumber"))(any(),any())
      } thenReturn Future.successful(feeResponse)

      whenReady(FeeConnector.feeResponse(amlsRegistrationNumber)){
        _ mustBe feeResponse
      }
    }

    "return sa account type and reference" in {
      implicit val saAcct = AuthContext(
        LoggedInUser(
          "UserName",
          None,
          None,
          None,
          CredentialStrength.Weak,
          ConfidenceLevel.L50),
        Principal(
          None,
          Accounts(sa = Some(SaAccount("Link", SaUtr("saRef"))))),
        None,
        None,
        None)
      FeeConnector.accountTypeAndId(saAcct) must be("sa","saRef")
    }

    "return ct account type and reference" in {
      implicit val ctAcct = AuthContext(
        LoggedInUser(
          "UserName",
          None,
          None,
          None,
          CredentialStrength.Weak,
          ConfidenceLevel.L50),
        Principal(
          None,
          Accounts(ct = Some(CtAccount("Link", CtUtr("ctRef"))))),
        None,
        None,
        None)
      FeeConnector.accountTypeAndId(ctAcct) must be("ct","ctRef")
    }

    "fail in not finding correct accountType" in {
      implicit val ctAcct = AuthContext(
        LoggedInUser(
          "UserName",
          None,
          None,
          None,
          CredentialStrength.Weak,
          ConfidenceLevel.L50),
        Principal(
          None,
          Accounts(ct = None)),
        None,
        None,
        None)
      an[IllegalArgumentException] should be thrownBy FeeConnector.accountTypeAndId(ctAcct)
    }

  }
}
