package connectors

import models._
import models.declaration.release7.RoleWithinBusinessRelease7
import models.declaration.{AddPerson, BeneficialShareholder}
import models.renewal.RenewalResponse
import org.joda.time.LocalDateTime
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object AmlsConnector extends AmlsConnector {

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
    aboutYouSection = AddPerson("FirstName", None, "LastName", RoleWithinBusinessRelease7(Set(models.declaration.release7.BeneficialShareholder))),
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
    fpFeeRate = None,
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = ""
  )

  val amendmentResponse = AmendVariationResponse(
    processingDate = "",
    etmpFormBundleNumber = "",
    registrationFee = 0,
    fpFee = Some(0),
    fpFeeRate = None,
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = Some(""),
    difference = Some(0)
  )
  val renewalResponse = RenewalResponse(
    processingDate = "",
    etmpFormBundleNumber = "",
    registrationFee = 0,
    fpFee = Some(0),
    fpFeeRate = None,
    premiseFee = 0,
    premiseFeeRate = None,
    totalFees = 0,
    paymentReference = Some(""),
    difference = Some(0)
  )



  val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Approved", None, None, None, None, false)

  implicit val hc = HeaderCarrier()
  implicit val ac = AuthContext(
    LoggedInUser(
      "UserName",
      None,
      None,
      None,
      CredentialStrength.Weak,
      ConfidenceLevel.L50,""),
    Principal(
      None,
      Accounts(org = Some(OrgAccount("Link", Org("TestOrgRef"))))),
    None,
    None,
    None,None)

  "subscribe" must {

    "successfully subscribe" in {

      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, SubscriptionResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$safeId"), eqTo(subscriptionRequest), any())(any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      whenReady(AmlsConnector.subscribe(subscriptionRequest, safeId)) {
        _ mustBe subscriptionResponse
      }
    }
  }

  "get status" must {

    "return correct status" in {
      when {
        AmlsConnector.httpGet.GET[ReadStatusResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/status"))(any(),any())
      } thenReturn Future.successful(readStatusResponse)

      whenReady(AmlsConnector.status(amlsRegistrationNumber)){
        _ mustBe readStatusResponse
      }
    }
  }

  "get view" must {

    "a view response" in {
      when {
        AmlsConnector.httpGet.GET[ViewResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber"))(any(),any())
      } thenReturn Future.successful(viewResponse)

      whenReady(AmlsConnector.view(amlsRegistrationNumber)){
        _ mustBe viewResponse
      }
    }
  }

  "update" must {

    "successfully submit amendment" in {
      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, AmendVariationResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/update")
          , eqTo(subscriptionRequest), any())(any(), any(), any())
      }.thenReturn(Future.successful(amendmentResponse))

      whenReady(AmlsConnector.update(subscriptionRequest,amlsRegistrationNumber)){
        _ mustBe amendmentResponse
      }
    }
  }

  "variation" must {
    "successfully submit variation" in {
      when {
        AmlsConnector.httpPost.POST[SubscriptionRequest, AmendVariationResponse](eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/variation")
          , eqTo(subscriptionRequest), any())(any(), any(), any())
      }.thenReturn(Future.successful(amendmentResponse))

      whenReady(AmlsConnector.variation(subscriptionRequest, amlsRegistrationNumber)) {
        _ mustBe amendmentResponse
      }
    }
  }

  "renewal" must {
    "successfully submit renewal" in {
      when { AmlsConnector.httpPost.POST[SubscriptionRequest, RenewalResponse](
          eqTo(s"${AmlsConnector.url}/org/TestOrgRef/$amlsRegistrationNumber/renewal"), eqTo(subscriptionRequest), any())(any(), any(), any())
      } thenReturn Future.successful(renewalResponse)

      whenReady(AmlsConnector.renewal(subscriptionRequest, amlsRegistrationNumber)) {
        _ mustBe renewalResponse
      }
    }

  }
}
