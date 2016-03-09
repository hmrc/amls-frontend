package services

import connectors.{DataCacheConnector, DESConnector}
import models.SubscriptionRequest
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessmatching.BusinessMatching
import models.estateagentbusiness.EstateAgentBusiness
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{NotFoundException, HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait SubscriptionService extends DataCacheService {

  private[services] def cacheConnector: DataCacheConnector
  private[services] def desConnector: DESConnector

  def subscribe
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[HttpResponse] = {
    getCache flatMap {
      cache =>
        cache.getEntry[BusinessMatching](BusinessMatching.key) flatMap {
          _.reviewDetails
        } map {
          reviewDetails =>
            val request = SubscriptionRequest(
              businessType =
                for {
                  businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
                  reviewDetails <- businessMatching.reviewDetails
                  businessType <- reviewDetails.businessType
                } yield businessType,
              eabSection = cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key),
              aboutTheBusinessSection = cache.getEntry[AboutTheBusiness](AboutTheBusiness.key),
              tradingPremisesSection = cache.getEntry[Seq[TradingPremises]](TradingPremises.key),
              bankDetailsSection = cache.getEntry[Seq[BankDetails]](BankDetails.key)
            )
            desConnector.subscribe(request, reviewDetails.safeId)
        } getOrElse Future.failed {
          new NotFoundException("No subscription data found for user")
        }
    }
  }
}

object SubscriptionService extends SubscriptionService {
  override private[services] val cacheConnector = DataCacheConnector
  override private[services] val desConnector = DESConnector
}
