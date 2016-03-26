package services

import connectors.{DESConnector, DataCacheConnector}
import models.SubscriptionRequest
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.tradingpremises.TradingPremises
import play.api.libs.json.Json
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, NotFoundException}

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
//              businessType =
//                for {
//                  businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
//                  reviewDetails <- businessMatching.reviewDetails
//                  businessType <- reviewDetails.businessType
//                } yield businessType,
              businessMatchingSection = cache.getEntry[BusinessMatching](BusinessMatching.key),
              eabSection = cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key),
              tradingPremisesSection = cache.getEntry[Seq[TradingPremises]](TradingPremises.key),
              aboutTheBusinessSection = cache.getEntry[AboutTheBusiness](AboutTheBusiness.key),
              bankDetailsSection = cache.getEntry[Seq[BankDetails]](BankDetails.key),
              aboutYouSection = cache.getEntry[AddPerson](AddPerson.key),
              businessActivitiesSection = cache.getEntry[BusinessActivities](BusinessActivities.key)
            )
            println()
            println(Json.prettyPrint(Json.toJson(request)))
            println()
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
