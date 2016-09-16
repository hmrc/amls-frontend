package connectors

import models.declaration.{AddPerson, BeneficialShareholder, RoleWithinBusiness}
import models.{ReadStatusResponse, SubscriptionRequest, SubscriptionResponse, ViewResponse}
import org.joda.time.LocalDateTime
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, ConfidenceLevel, CredentialStrength, OrgAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DESConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object DESConnector extends DESConnector {
    override private[connectors] val httpPost: HttpPost = mock[HttpPost]
    override private[connectors] val url: String = "amls/subscription"
    override private[connectors] val httpGet: HttpGet = mock[HttpGet]
  }

  val safeId = "SAFEID"
  val amlsRegistrationNumber = "AMLSREGNO"

  val subscriptionRequest = SubscriptionRequest(
    businessMatchingSection = None,
    eabSection = None,
    tradingPremisesSection = None,
    aboutTheBusinessSection = None,
    bankDetailsSection = None,
    aboutYouSection = None,
    businessActivitiesSection = None,
    responsiblePeopleSection = None,
    tcspSection = None,
    aspSection = None,
    msbSection = None,
    hvdSection = None,
    supervisionSection = None
  )


  val viewResponse = ViewResponse(
    etmpFormBundleNumber = "FORMBUNDLENUMBER",
    businessMatchingSection = None,
    eabSection = None,
    tradingPremisesSection = None,
    aboutTheBusinessSection = None,
    bankDetailsSection = Seq(None),
    aboutYouSection = AddPerson("FirstName", None, "LastName", BeneficialShareholder ),
    businessActivitiesSection = None,
    responsiblePeopleSection = None,
    tcspSection = None,
    aspSection = None,
    msbSection = None,
    hvdSection = None,
    supervisionSection = None
  )

  val subscriptionResponse = SubscriptionResponse(
    etmpFormBundleNumber = "",
    amlsRefNo = "",
    registrationFee = 0,
    fpFee = None,
    premiseFee = 0,
    totalFees = 0,
    paymentReference = ""
  )

  val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, false)

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

  "subscribe" must {

    "successfully subscribe" in {

      when {
        DESConnector.httpPost.POST[SubscriptionRequest, SubscriptionResponse](eqTo(s"${DESConnector.url}/org/TestOrgRef/$safeId"), eqTo(subscriptionRequest), any())(any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      whenReady(DESConnector.subscribe(subscriptionRequest, safeId)) {
        _ mustBe subscriptionResponse
      }
    }
  }

  "get status" must {

    "return correct status" in {
      when {
        DESConnector.httpGet.GET[ReadStatusResponse](eqTo(s"${DESConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/status"))(any(),any())
      } thenReturn Future.successful(readStatusResponse)

      whenReady(DESConnector.status(amlsRegistrationNumber)){
        _ mustBe readStatusResponse
      }
    }
  }

  "get view" must {

    "a view response" in {
      when {
        DESConnector.httpGet.GET[ViewResponse](eqTo(s"${DESConnector.url}/org/TestOrgRef/$amlsRegistrationNumber"))(any(),any())
      } thenReturn Future.successful(viewResponse)

      whenReady(DESConnector.view(amlsRegistrationNumber)){
        _ mustBe viewResponse
      }
    }
  }
}
