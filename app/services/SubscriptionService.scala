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
  override private[services] val ggService = GovernmentGatewayService
}
