/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, BusinessMatchingConnector, DataCacheConnector, KeystoreConnector}
import models.{AmendVariationRenewalResponse, SubscriptionResponse, ViewResponse}
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.{BusinessActivities, ExpectedAMLSTurnover, ExpectedBusinessTurnover}
import models.businesscustomer.ReviewDetails
import models.businessmatching.{BusinessMatching, BusinessActivities => BMActivities}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.{ExpectedThroughput, MoneyServiceBusiness}
import models.renewal.{ReceiveCashPayments, _}
import models.responsiblepeople.ResponsiblePerson
import models.status.RenewalSubmitted
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.mvc.Request
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

trait LandingService {

  private[services] val cacheConnector: DataCacheConnector
  private[services] val keyStore: KeystoreConnector
  private[services] val desConnector: AmlsConnector
  private[services] val statusService: StatusService
  private[services] val businessMatchingConnector: BusinessMatchingConnector

  def cacheMap(implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[CacheMap]] = cacheConnector.fetchAll

  def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = cacheConnector.remove

  private def saveRenewalData(viewResponse: ViewResponse, cacheMap: CacheMap)
                             (implicit authContext: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {

    import models.businessactivities.{InvolvedInOther => BAInvolvedInOther}
    import models.hvd.{PercentageOfCashPaymentOver15000 => HvdRPercentageOfCashPaymentOver15000, ReceiveCashPayments => HvdReceiveCashPayments}
    import models.moneyservicebusiness.{WhichCurrencies => MsbWhichCurrencies}

    for {
      renewalStatus <- statusService.getStatus
    } yield renewalStatus match {
      case RenewalSubmitted(_) => {
        val renewal = Some(Renewal(
          involvedInOtherActivities = viewResponse.businessActivitiesSection.involvedInOther.map(i => BAInvolvedInOther.convert(i)),
          businessTurnover = viewResponse.businessActivitiesSection.expectedBusinessTurnover.map(data => ExpectedBusinessTurnover.convert(data)),
          turnover = viewResponse.businessActivitiesSection.expectedAMLSTurnover.map(data => ExpectedAMLSTurnover.convert(data)),
          customersOutsideUK = viewResponse.businessActivitiesSection.customersOutsideUK.map(c => CustomersOutsideUK(c.countries)),

          percentageOfCashPaymentOver15000 = viewResponse.hvdSection.fold[Option[PercentageOfCashPaymentOver15000]](None)
            (_.percentageOfCashPaymentOver15000.map(p => HvdRPercentageOfCashPaymentOver15000.convert(p))),

          receiveCashPayments = viewResponse.hvdSection.fold[Option[ReceiveCashPayments]](None) { hvd =>
            hvd.receiveCashPayments.map(r => HvdReceiveCashPayments.convert(hvd))
          },

          totalThroughput = viewResponse.msbSection.fold[Option[TotalThroughput]](None)(_.throughput.map(t => ExpectedThroughput.convert(t))),

          whichCurrencies = viewResponse.msbSection.fold[Option[WhichCurrencies]](None)(_.whichCurrencies.map(c => MsbWhichCurrencies.convert(c))),

          transactionsInLast12Months = viewResponse.msbSection.fold[Option[TransactionsInLast12Months]](None)
            (_.transactionsInNext12Months.map(t => TransactionsInLast12Months(t.txnAmount))),

          sendTheLargestAmountsOfMoney = viewResponse.msbSection.fold[Option[SendTheLargestAmountsOfMoney]](None)
            (_.sendTheLargestAmountsOfMoney.map(s => SendTheLargestAmountsOfMoney(s.country_1, s.country_2, s.country_3))),

          mostTransactions = viewResponse.msbSection.fold[Option[MostTransactions]](None)
            (_.mostTransactions.map(m => MostTransactions(m.countries))),

          ceTransactionsInLast12Months = viewResponse.msbSection.fold[Option[CETransactionsInLast12Months]](None)
            (_.ceTransactionsInNext12Months.map(t => CETransactionsInLast12Months(t.ceTransaction)))
        ))
        cacheConnector.updateCacheEntity[Renewal](Some(cacheMap), Renewal.key, renewal)
      }
      case _ => cacheMap
    }
  }

  private def fixAddress(model: AboutTheBusiness) = (model.correspondenceAddress, model.altCorrespondenceAddress) match {
    case (Some(_), None) => model.copy(altCorrespondenceAddress = Some(true), hasAccepted = true)
    case (None, None) => model.copy(altCorrespondenceAddress = Some(false), hasAccepted = true)
    case _ => model
  }

  def setAltCorrespondenceAddress(amlsRefNumber: String, maybeCacheMap: Option[CacheMap])
                                 (implicit authContext: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    val cachedModel = for {
      cache <- OptionT.fromOption[Future](maybeCacheMap)
      entry <- OptionT.fromOption[Future](cache.getEntry[AboutTheBusiness](AboutTheBusiness.key))
    } yield entry

    lazy val etmpModel = OptionT.liftF(desConnector.view(amlsRefNumber) map { v => v.aboutTheBusinessSection })

    (for {
      aboutTheBusiness <- cachedModel orElse etmpModel
      cacheMap <- OptionT.liftF(cacheConnector.save[AboutTheBusiness](AboutTheBusiness.key, fixAddress(aboutTheBusiness)))
    } yield cacheMap) getOrElse (throw new Exception("Unable to update alt correspondence address"))
  }

  def setAltCorrespondenceAddress(aboutTheBusiness: AboutTheBusiness)(implicit
                                                                      authContext: AuthContext,
                                                                      hc: HeaderCarrier,
                                                                      ec: ExecutionContext
  ): Future[CacheMap] = {
    cacheConnector.save[AboutTheBusiness](AboutTheBusiness.key, fixAddress(aboutTheBusiness))
  }

  def refreshCache(amlsRefNumber: String)
                  (implicit authContext: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {
    for {
      // MUST clear cash first to remove stale data and reload from API5
      viewResponse           <- desConnector.view(amlsRefNumber)
      subscriptionResponse   <- cacheConnector.fetch[SubscriptionResponse](SubscriptionResponse.key).recover { case _ => None }
      amendVariationResponse <- cacheConnector.fetch[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key) recover { case _ => None }
      isReset                <- cacheConnector.remove
      appCache               <- cacheConnector.fetchAll
      refreshedCache         <- {
        val cache1  = cacheConnector.updateCacheEntity[Option[ViewResponse]](appCache, ViewResponse.key, Some(viewResponse))
        val cache2  = cacheConnector.updateCacheEntity[BusinessMatching](Some(cache1), BusinessMatching.key, Some(businessMatchingSection(viewResponse.businessMatchingSection)))
        val cache3  = cacheConnector.updateCacheEntity[Option[EstateAgentBusiness]](Some(cache2), EstateAgentBusiness.key, Some(viewResponse.eabSection.copy(hasAccepted = true)))
        val cache4  = cacheConnector.updateCacheEntity[Option[Seq[TradingPremises]]](Some(cache3), TradingPremises.key, tradingPremisesSection(viewResponse.tradingPremisesSection))
        val cache5  = cacheConnector.updateCacheEntity[AboutTheBusiness](Some(cache4), AboutTheBusiness.key, viewResponse.aboutTheBusinessSection.copy(hasAccepted = true))
        val cache6  = cacheConnector.updateCacheEntity[Seq[BankDetails]](Some(cache5), BankDetails.key, writeEmptyBankDetails(viewResponse.bankDetailsSection))
        val cache7  = cacheConnector.updateCacheEntity[AddPerson](Some(cache6), AddPerson.key, viewResponse.aboutYouSection)
        val cache8  = cacheConnector.updateCacheEntity[BusinessActivities](Some(cache7), BusinessActivities.key, Some(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))
        val cache9  = cacheConnector.updateCacheEntity[Option[Tcsp]](Some(cache8), Tcsp.key, Some(viewResponse.tcspSection.copy(hasAccepted = true)))
        val cache10 = cacheConnector.updateCacheEntity[Option[Asp]](Some(cache9), Asp.key, Some(viewResponse.aspSection.copy(hasAccepted = true)))
        val cache11 = cacheConnector.updateCacheEntity[Option[MoneyServiceBusiness]](Some(cache10), MoneyServiceBusiness.key, Some(viewResponse.msbSection.copy(hasAccepted = true)))
        val cache12 = cacheConnector.updateCacheEntity[Option[Hvd]](Some(cache11), Hvd.key, Some(viewResponse.hvdSection.copy(hasAccepted = true)))
        val cache13 = cacheConnector.updateCacheEntity[Option[Supervision]](Some(cache12), Supervision.key, Some(viewResponse.supervisionSection.copy(hasAccepted = true)))
        val cache14 = cacheConnector.updateCacheEntity[Option[SubscriptionResponse]](Some(cache13), SubscriptionResponse.key, subscriptionResponse)
        val cache15 = cacheConnector.updateCacheEntity[Option[AmendVariationRenewalResponse]](Some(cache14), AmendVariationRenewalResponse.key, amendVariationResponse)
        val cache16 = cacheConnector.updateCacheEntity[Option[Seq[ResponsiblePerson]]](Some(cache15), ResponsiblePerson.key, responsiblePeopleSection(viewResponse.responsiblePeopleSection))
        val cache17 = saveRenewalData(viewResponse, cache16)
        cacheConnector.saveAll(cache17)
      }
    } yield refreshedCache
  }

  def businessMatchingSection(viewResponse: BusinessMatching): BusinessMatching = {
    viewResponse.copy(
      activities = viewResponse.activities.fold(viewResponse.activities){ activities =>
        Some(BMActivities(
          activities.businessActivities,
          activities.additionalActivities,
          None,
          activities.dateOfChange
        ))
      },
      hasAccepted = true,
      preAppComplete = true
    )
  }

  def responsiblePeopleSection(viewResponse: Option[Seq[ResponsiblePerson]]): Option[Seq[ResponsiblePerson]] =
    Some(viewResponse.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))

  def tradingPremisesSection(viewResponse: Option[Seq[TradingPremises]]): Option[Seq[TradingPremises]] =
    Some(viewResponse.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))

  def writeEmptyBankDetails(bankDetailsSeq: Seq[BankDetails]): Seq[BankDetails] = {
    val empty = Seq.empty[BankDetails]
    bankDetailsSeq match {
      case `empty` => Seq(BankDetails(None, None, None, false, true, None, true))
      case _ =>
        bankDetailsSeq map {
          bank => bank.copy(hasAccepted = true)
        }
    }
  }

  def reviewDetails(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Future[Option[ReviewDetails]] = {
    businessMatchingConnector.getReviewDetails map {
      case Some(details) => Some(ReviewDetails.convert(details))
      case _ => None
    }
  }

  /* Consider if there's a good way to stop
   * this from just overwriting whatever is in Business Matching,
   * shouldn't be a problem as this should only happen when someone
   * first comes into the Application from Business Customer FE
   */
  def updateReviewDetails(reviewDetails: ReviewDetails)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[CacheMap] = {
    val bm = BusinessMatching(reviewDetails = Some(reviewDetails))
    cacheConnector.save[BusinessMatching](BusinessMatching.key, bm)
  }
}

object LandingService extends LandingService {
  // $COVERAGE-OFF$
  override private[services] lazy val cacheConnector = DataCacheConnector
  override private[services] lazy val keyStore = KeystoreConnector
  override private[services] lazy val desConnector = AmlsConnector
  override private[services] lazy val statusService = StatusService
  override private[services] lazy val businessMatchingConnector = BusinessMatchingConnector
  // $COVERAGE-ON$
}
