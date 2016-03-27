package services

import connectors.{DESConnector, DataCacheConnector}
import models.{SubscriptionRequest, SubscriptionResponse}
import models.aboutthebusiness.AboutTheBusiness
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import models.confirmation.{BreakdownRow, Currency}
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

  private object Submission {
    val message = "confirmation.submission"
    val quantity = 1
    val feePer = 100
  }

  private object Premises {
    val message = "confirmation.tradingpremises"
    val feePer = 110
  }

  def subscribe
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[SubscriptionResponse] = {
    getCache flatMap {
      cache =>
        cache.getEntry[BusinessMatching](BusinessMatching.key) flatMap {
          _.reviewDetails
        } map {
          reviewDetails =>
            val request = SubscriptionRequest(
              businessMatchingSection = cache.getEntry[BusinessMatching](BusinessMatching.key),
              eabSection = cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key),
              tradingPremisesSection = cache.getEntry[Seq[TradingPremises]](TradingPremises.key),
              aboutTheBusinessSection = cache.getEntry[AboutTheBusiness](AboutTheBusiness.key),
              bankDetailsSection = cache.getEntry[Seq[BankDetails]](BankDetails.key),
              aboutYouSection = cache.getEntry[AddPerson](AddPerson.key),
              businessActivitiesSection = cache.getEntry[BusinessActivities](BusinessActivities.key)
            )
            for {
              response <- desConnector.subscribe(request, reviewDetails.safeId)
              _ <- cacheConnector.save[SubscriptionResponse](SubscriptionResponse.key, response)
            } yield response
        } getOrElse Future.failed {
          new NotFoundException("No subscription data found for user")
        }
    }
  }

  def getSubscription
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[(String, Currency, Seq[BreakdownRow])] =
    cacheConnector.fetchAll flatMap {
      option =>
        (for {
          cache <- option
          subscription <- cache.getEntry[SubscriptionResponse](SubscriptionResponse.key)
          premises <- cache.getEntry[Seq[TradingPremises]](TradingPremises.key)
        } yield {
          val mlrRegNo = subscription.amlsRefNo
          val total = subscription.totalFees
          val rows = Seq(
            BreakdownRow(Submission.message, Submission.quantity, Submission.feePer, subscription.registrationFee),
            BreakdownRow(Premises.message, premises.size, Premises.feePer, subscription.premiseFee)
          )
          Future.successful((mlrRegNo, Currency.fromBD(total), rows))
          // TODO
        }) getOrElse Future.failed(new Exception("TODO"))
    }
}

object SubscriptionService extends SubscriptionService {
  override private[services] val cacheConnector = DataCacheConnector
  override private[services] val desConnector = DESConnector
}
