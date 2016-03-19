package services

import connectors.{GovernmentGatewayConnector, DataCacheConnector, DESConnector}
import models.governmentgateway.EnrolmentRequest
import models.{SubscriptionResponse, SubscriptionRequest}
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessmatching.BusinessMatching
import models.estateagentbusiness.EstateAgentBusiness
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.{GovernmentGateway, AuthContext}
import uk.gov.hmrc.play.http.{NotFoundException, HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait SubscriptionService extends DataCacheService {

  private[services] def cacheConnector: DataCacheConnector
  private[services] def desConnector: DESConnector
  private[services] def ggService: GovernmentGatewayService

  private def safeId(cache: CacheMap): Future[String] = {
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      rd <- bm.reviewDetails
    } yield rd.safeId) match {
      case Some(a) =>
        Future.successful(a)
      case _ =>
        // TODO: Better exception
        Future.failed(new Exception(""))
    }
  }

  private def businessType(cache: CacheMap): Option[String] =
    for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      rd <- bm.reviewDetails
      bt <- rd.businessType
    } yield bt

  private def subscribe
  (cache: CacheMap, safeId: String)
  (implicit
   ac: AuthContext,
   hc: HeaderCarrier
  ): Future[SubscriptionResponse] = {
    val request = SubscriptionRequest(
      businessType = businessType(cache),
      eabSection = cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key),
      aboutTheBusinessSection = cache.getEntry[AboutTheBusiness](AboutTheBusiness.key),
      tradingPremisesSection = cache.getEntry[Seq[TradingPremises]](TradingPremises.key),
      bankDetailsSection = cache.getEntry[Seq[BankDetails]](BankDetails.key)
    )
    desConnector.subscribe(request, safeId)
  }

  def subscribe
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[SubscriptionResponse] = {
    for {
      cache <- getCache
      safeId <- safeId(cache)
      subscription <- subscribe(cache, safeId)
      _ <- ggService.enrol(
        safeId = safeId,
        mlrRefNo = subscription.amlsRefNo
      )
    } yield subscription
  }
}

object SubscriptionService extends SubscriptionService {
  override private[services] val cacheConnector = DataCacheConnector
  override private[services] val desConnector = DESConnector
  override private[services] val ggService = GovernmentGatewayService
}
