package services

import connectors.{AmlsConnector, DataCacheConnector}
import exceptions.NoEnrolmentException
import models.SubscriptionResponse
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.SoleProprietor
import models.estateagentbusiness.EstateAgentBusiness
import models.tradingpremises.TradingPremises
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, OrgAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Principal}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class SubmissionServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience {

  trait Fixture {

    object SubmissionService extends SubmissionService {
      override private[services] val cacheConnector = mock[DataCacheConnector]
      override private[services] val amlsConnector = mock[AmlsConnector]
      override private[services] val ggService = mock[GovernmentGatewayService]
      override private[services] val authEnrolmentsService = mock[AuthEnrolmentsService]
    }

    implicit val authContext = mock[AuthContext]
    val principle = Principal(None, Accounts(org = Some(OrgAccount("", Org("TestOrgRef")))))
    when {
      authContext.principal
    }.thenReturn(principle)

    implicit val headerCarrier = HeaderCarrier()

    val enrolmentResponse = HttpResponse(OK)

    val subscriptionResponse = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "amlsRef",
      registrationFee = 0,
      fpFee = None,
      premiseFee = 0,
      totalFees = 0,
      paymentReference = ""
    )

    val safeId = "safeId"
    val amlsRegistrationNumber = "amlsRegNo"
    val businessType = SoleProprietor

    val reviewDetails = mock[ReviewDetails]

    when {
      reviewDetails.safeId
    } thenReturn safeId
    when {
      reviewDetails.businessType
    } thenReturn Some(businessType)

    val businessMatching = mock[BusinessMatching]

    when {
      businessMatching.reviewDetails
    } thenReturn Some(reviewDetails)

    val cache = mock[CacheMap]

    when {
      cache.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(businessMatching)
    when {
      cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)
    } thenReturn Some(mock[EstateAgentBusiness])
    when {
      cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
    } thenReturn Some(mock[AboutTheBusiness])
    when {
      cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
    } thenReturn Some(mock[Seq[TradingPremises]])
    when {
      cache.getEntry[Seq[BankDetails]](BankDetails.key)
    } thenReturn Some(mock[Seq[BankDetails]])
  }

  "SubmissionService" must {

    "successfully subscribe and enrol" in new Fixture {

      when {
        SubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        SubmissionService.cacheConnector.save[SubscriptionResponse](eqTo(SubscriptionResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        SubmissionService.amlsConnector.subscribe(any(), eqTo(safeId))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      when {
        SubmissionService.ggService.enrol(eqTo("amlsRef"), eqTo(safeId))(any(), any())
      } thenReturn Future.successful(enrolmentResponse)

      whenReady(SubmissionService.subscribe) {
        result =>
          result must equal(subscriptionResponse)
      }
    }

    "successfully submit amendment" in new Fixture {

      when {
        SubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        SubmissionService.cacheConnector.save[SubscriptionResponse](eqTo(SubscriptionResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        SubmissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      when {
        SubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      }.thenReturn(Future.successful(Some(amlsRegistrationNumber)))


      whenReady(SubmissionService.update) {
        result =>
          result must equal(subscriptionResponse)
      }
    }

    "return failed future when no enrolment" in new Fixture {

      when {
        SubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        SubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      }.thenReturn(Future.successful(None))


      whenReady(SubmissionService.update.failed) {
        result =>
          result mustBe a[NoEnrolmentException]
      }
    }
  }
}
