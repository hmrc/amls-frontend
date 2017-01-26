package services

import connectors.{AmlsConnector, DataCacheConnector, KeystoreConnector}
import models.{Country, ViewResponse}
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.declaration.{AddPerson, BeneficialShareholder}
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import play.api.libs.json.Writes
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class LandingServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object TestLandingService extends LandingService {
    override private[services] val cacheConnector = mock[DataCacheConnector]
    override private[services] val keyStore = mock[KeystoreConnector]
    override private[services] val desConnector = mock[AmlsConnector]
  }

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]
  implicit val ec = mock[ExecutionContext]

  "hasSavedFrom" must {

    "return true if a cache exists" in {
      when {
        TestLandingService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(CacheMap("", Map.empty)))
      whenReady (TestLandingService.hasSavedForm) {
        _ mustEqual true
      }
    }

    "return false if a cache does not exist" in {
      when {
        TestLandingService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(None)
      whenReady (TestLandingService.hasSavedForm) {
        _ mustEqual false
      }
    }
  }

  "refreshCache" must {

    val cacheMap = CacheMap("", Map.empty)
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

    def setUpMockView[T](mock: DataCacheConnector, result: CacheMap, key: String, section : T) = {
      when {
        mock.save[T](eqTo(key), eqTo(section))(any(), any(), any())
      } thenReturn Future.successful(result)
    }

    "return a cachMap of the saved sections" in {
      when {
        TestLandingService.desConnector.view(any[String])(any[HeaderCarrier], any[ExecutionContext], any[Writes[ViewResponse]], any[AuthContext])
      } thenReturn Future.successful(viewResponse)
      val user = mock[LoggedInUser]
      when(ac.user).thenReturn(user)
      when(user.oid).thenReturn("")
      when(TestLandingService.cacheConnector.remove(any())(any())).thenReturn(Future.successful(HttpResponse(200)))
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BusinessMatching.key, viewResponse.businessMatchingSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, EstateAgentBusiness.key, viewResponse.eabSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, TradingPremises.key, viewResponse.tradingPremisesSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, AboutTheBusiness.key, viewResponse.aboutTheBusinessSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BankDetails.key, viewResponse.bankDetailsSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, AddPerson.key, viewResponse.aboutYouSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, BusinessActivities.key, viewResponse.businessActivitiesSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, ResponsiblePeople.key, viewResponse.responsiblePeopleSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Tcsp.key, viewResponse.tcspSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Asp.key, viewResponse.aspSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, MoneyServiceBusiness.key, viewResponse.msbSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Hvd.key, viewResponse.hvdSection)
      setUpMockView(TestLandingService.cacheConnector, cacheMap, Supervision.key, viewResponse.supervisionSection)

      whenReady(TestLandingService.refreshCache("regNo")){
        _ mustEqual cacheMap
      }
    }

  }

  "reviewDetails" must {

    "pass through from the keystore connector" in {
      when {
        TestLandingService.keyStore.optionalReviewDetails(any(), any())
      } thenReturn Future.successful(None)
      whenReady (TestLandingService.reviewDetails) {
        _ mustEqual None
      }
    }
  }

  "updateReviewDetails" must {

    val cacheMap = CacheMap("", Map.empty)
    val reviewDetails = ReviewDetails(
      businessName = "",
      businessType = None,
      businessAddress = Address(
        line_1 = "",
        line_2 = "",
        line_3 = None,
        line_4 = None,
        postcode = None,
        country = Country("United Kingdom", "GB")
      ),
      safeId = ""
    )

    "save BusinessMatching and AboutTheBusiness when both succeed" in {
      when {
        TestLandingService.cacheConnector.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
      } thenReturn Future.successful(cacheMap)
      when {
        TestLandingService.cacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any())(any(), any(), any())
      } thenReturn Future.successful(cacheMap)
      whenReady (TestLandingService.updateReviewDetails(reviewDetails)) {
        _ mustEqual cacheMap
      }
    }

    "pass back a failed future when updating BusinessMatching fails" in {
      when {
        TestLandingService.cacheConnector.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
      } thenReturn Future.failed(new Exception(""))
      when {
        TestLandingService.cacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any())(any(), any(), any())
      } thenReturn Future.successful(cacheMap)
      whenReady (TestLandingService.updateReviewDetails(reviewDetails).failed) {
        _ mustBe an[Exception]
      }
    }

    "pass back a failed future when updating AboutTheBusiness fails" in {
      when {
        TestLandingService.cacheConnector.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any())
      } thenReturn Future.successful(cacheMap)
      when {
        TestLandingService.cacheConnector.save[AboutTheBusiness](eqTo(AboutTheBusiness.key), any())(any(), any(), any())
      } thenReturn Future.failed(new Exception)
      whenReady (TestLandingService.updateReviewDetails(reviewDetails).failed) {
        _ mustBe an[Exception]
      }
    }
  }
}
