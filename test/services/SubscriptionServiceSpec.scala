package services

import connectors.{DESConnector, DataCacheConnector}
import models.SubscriptionResponse
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businesscustomer.ReviewDetails
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.SoleProprietor
import models.estateagentbusiness.EstateAgentBusiness
import models.governmentgateway.EnrolmentResponse
import models.tradingpremises.TradingPremises
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class SubscriptionServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  trait Fixture {

    object SubscriptionService extends SubscriptionService {
      override private[services] val cacheConnector = mock[DataCacheConnector]
      override private[services] val desConnector = mock[DESConnector]
      override private[services] val ggService = mock[GovernmentGatewayService]
    }

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val enrolmentResponse = EnrolmentResponse(
      serviceName = "",
      state = "",
      friendlyName = "",
      identifiersForDisplay = Seq.empty
    )

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

  "SubscriptionService" must {

    "successfully subscribe and enrol" in new Fixture {

      when {
        SubscriptionService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cache))

      when {
        SubscriptionService.desConnector.subscribe(any(), eqTo(safeId))(any())
      } thenReturn Future.successful(subscriptionResponse)

      when {
        SubscriptionService.ggService.enrol(eqTo("amlsRef"), eqTo(safeId))(any())
      } thenReturn Future.successful(enrolmentResponse)

      whenReady (SubscriptionService.subscribe) {
        result =>
          result must equal (subscriptionResponse)
      }
    }
  }
}
