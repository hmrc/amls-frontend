package services

import connectors.{AmlsConnector, DataCacheConnector}
import exceptions.NoEnrolmentException
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.SoleProprietor
import models.confirmation.{BreakdownRow, Currency}
import models.estateagentbusiness.EstateAgentBusiness
import models.responsiblepeople.ResponsiblePeople
import models.tradingpremises.TradingPremises
import models.{AmendVariationResponse, SubscriptionResponse}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.http.Status._
import play.api.test.FakeApplication
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, OrgAccount}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Principal}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class SubmissionServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with IntegrationPatience with OneAppPerSuite {

  implicit override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.amounts.registration" -> 100))

  trait Fixture {

    val TestSubmissionService = new SubmissionService {
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

    val amendmentResponse = AmendVariationResponse(
      processingDate = "",
      etmpFormBundleNumber = "",
      registrationFee = 100,
      fpFee = Some(0),
      premiseFee = 0,
      totalFees = 100,
      paymentReference = Some(""),
      difference = Some(0)
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
      cache.getEntry[Seq[BankDetails]](BankDetails.key)
    } thenReturn Some(mock[Seq[BankDetails]])
    when {
      cache.getEntry[AmendVariationResponse](AmendVariationResponse.key)
    } thenReturn Some(amendmentResponse)
  }

  "SubmissionService" must {

    "successfully subscribe and enrol" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.cacheConnector.save[SubscriptionResponse](eqTo(SubscriptionResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.subscribe(any(), eqTo(safeId))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(subscriptionResponse)

      when {
        TestSubmissionService.ggService.enrol(eqTo("amlsRef"), eqTo(safeId))(any(), any())
      } thenReturn Future.successful(enrolmentResponse)

      whenReady(TestSubmissionService.subscribe) {
        result =>
          result must equal(subscriptionResponse)
      }
    }

    "successfully submit amendment" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      }.thenReturn(Future.successful(Some(amlsRegistrationNumber)))


      whenReady(TestSubmissionService.update) {
        result =>
          result must equal(amendmentResponse)
      }
    }

    "successfully submit amendment returning submission data" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some("12345"))

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(Seq(TradingPremises()))

      when {
        cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
      } thenReturn Some(Seq(ResponsiblePeople()))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      val rows = Seq(
        BreakdownRow("confirmation.submission", 1, 100, 100)
      ) ++ Seq(
        BreakdownRow("confirmation.responsiblepeople", 1, 100, 0)
      ) ++ Seq(
        BreakdownRow("confirmation.tradingpremises", 1, 115, 0)
      )

      val response = Some("12345", Currency.fromBD(100), rows, Some(Currency.fromBD(0)))

      whenReady(TestSubmissionService.getAmendment) {
        result =>
          result must equal(response)
      }
    }

    "retrieve data from variation submission" in new Fixture {

      val variationResponse = AmendVariationResponse(
        processingDate = "",
        etmpFormBundleNumber = "",
        registrationFee = 100,
        fpFee = Some(0),
        premiseFee = 0,
        totalFees = 100,
        paymentReference = Some(""),
        difference = Some(0),
        addedResponsiblePeople = 1,
        addedFullYearTradingPremises = 1,
        halfYearlyTradingPremises = 3,
        zeroRatedTradingPremises = 1
      )

      val rpFee: Double = 100
      val tpFee: Double = 115
      val tpHalfFee: Double = tpFee/2
      val tpTotalFee: Double = tpFee + (tpHalfFee * 3)
      val totalFee: Double = rpFee + tpTotalFee

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(Some("12345"))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        cache.getEntry[AmendVariationResponse](any())(any())
      } thenReturn Some(variationResponse)

      val rows = Seq(
        BreakdownRow("confirmation.responsiblepeople", 1, 100, Currency(rpFee))
      ) ++ Seq(
        BreakdownRow("confirmation.tradingpremises", 5, 115, Currency(tpTotalFee))
      )

      val response = Some("12345", Currency.fromBD(totalFee), rows, None)

      whenReady(TestSubmissionService.getVariation) {
        result =>
          result must equal(response)
      }

    }

    "return None if data cannot be returned containing AMLS Reg No" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      } thenReturn Future.successful(None)

      when {
        cache.getEntry[Seq[TradingPremises]](eqTo(TradingPremises.key))(any())
      } thenReturn Some(Seq(TradingPremises()))

      when {
        cache.getEntry[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any())
      } thenReturn Some(Seq(ResponsiblePeople()))

      when {
        TestSubmissionService.cacheConnector.save[AmendVariationResponse](eqTo(AmendVariationResponse.key), any())(any(), any(), any())
      } thenReturn Future.successful(CacheMap("", Map.empty))

      when {
        TestSubmissionService.amlsConnector.update(any(), eqTo(amlsRegistrationNumber))(any(), any(), any(), any(), any())
      } thenReturn Future.successful(amendmentResponse)

      whenReady(TestSubmissionService.getAmendment) {
        result =>
          result must equal(None)
      }
    }

    "return failed future when no enrolment" in new Fixture {

      when {
        TestSubmissionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        TestSubmissionService.authEnrolmentsService.amlsRegistrationNumber(any(), any(), any())
      }.thenReturn(Future.successful(None))


      whenReady(TestSubmissionService.update.failed) {
        result =>
          result mustBe a[NoEnrolmentException]
      }
    }
  }
}
