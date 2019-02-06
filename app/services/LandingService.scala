/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Play
import play.api.mvc.Request
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

trait LandingService {

  private[services] val cacheConnector: DataCacheConnector
  private[services] val keyStore: KeystoreConnector
  private[services] val desConnector: AmlsConnector
  private[services] val statusService: StatusService
  private[services] val businessMatchingConnector: BusinessMatchingConnector

  def cacheMap(implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[Option[CacheMap]] = cacheConnector.fetchAll

  def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = cacheConnector.remove

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
      viewResponse           <- desConnector.view(amlsRefNumber)
      subscriptionResponse   <- cacheConnector.fetch[SubscriptionResponse](SubscriptionResponse.key).recover { case _ => None }
      amendVariationResponse <- cacheConnector.fetch[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key) recover { case _ => None }
      _                      <- cacheConnector.remove // MUST clear cash first to remove stale data and reload from API5
      appCache               <- cacheConnector.fetchAllWithDefault
      refreshedCache         <- {
       upsertCacheEntries(appCache, viewResponse, subscriptionResponse, amendVariationResponse)
      }
    } yield refreshedCache
  }

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

  /* **********
   * Privates *
   ************/

  private def upsertCacheEntries(appCache: CacheMap, viewResponse: ViewResponse, subscriptionResponse: Option[SubscriptionResponse],
                                 amendVariationResponse: Option[AmendVariationRenewalResponse])
                                (implicit authContext: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = {

    val cachedViewResponse = cacheConnector.upsert[Option[ViewResponse]](appCache, ViewResponse.key, Some(viewResponse))

    val cachedBusinessMatching = cacheConnector.upsert[BusinessMatching](cachedViewResponse, BusinessMatching.key,
      viewResponseSection(viewResponse))

    val cachedEstateAgentBusiness = cacheConnector.upsert[Option[EstateAgentBusiness]](cachedBusinessMatching,
      EstateAgentBusiness.key, eabSection(viewResponse))

    val cachedTradingPremises = cacheConnector.upsert[Option[Seq[TradingPremises]]](cachedEstateAgentBusiness, TradingPremises.key,
      tradingPremisesSection(viewResponse.tradingPremisesSection))

    val cachedAboutTheBusiness = cacheConnector.upsert[AboutTheBusiness](cachedTradingPremises, AboutTheBusiness.key, aboutSection(viewResponse))

    val cachedBankDetails = cacheConnector.upsert[Seq[BankDetails]](
      cachedAboutTheBusiness, BankDetails.key, writeEmptyBankDetails(viewResponse.bankDetailsSection)
    )
    val cachedAddPerson = cacheConnector.upsert[AddPerson](cachedBankDetails, AddPerson.key, viewResponse.aboutYouSection)

    val cachedBusinessActivities = cacheConnector.upsert[BusinessActivities](cachedAddPerson, BusinessActivities.key, activitySection(viewResponse))

    val cachedTcsp = cacheConnector.upsert[Option[Tcsp]](cachedBusinessActivities, Tcsp.key, tcspSection(viewResponse))

    val cachedAsp = cacheConnector.upsert[Option[Asp]](cachedTcsp, Asp.key, aspSection(viewResponse))

    val cachedMoneyServiceBusiness = cacheConnector.upsert[Option[MoneyServiceBusiness]](cachedAsp, MoneyServiceBusiness.key, msbSection(viewResponse))

    val cachedHvd = cacheConnector.upsert[Option[Hvd]](cachedMoneyServiceBusiness, Hvd.key, hvdSection(viewResponse))

    val cachedSupervision = cacheConnector.upsert[Option[Supervision]](cachedHvd, Supervision.key, supervisionSection(viewResponse))

    val cachedSubscriptionResponse = cacheConnector.upsert[Option[SubscriptionResponse]](cachedSupervision,
      SubscriptionResponse.key, subscriptionResponse)

    val cachedAmendVariationRenewalResponse = cacheConnector.upsert[Option[AmendVariationRenewalResponse]](cachedSubscriptionResponse,
      AmendVariationRenewalResponse.key, amendVariationResponse)

    val cachedResponsiblePerson = cacheConnector.upsert[Option[Seq[ResponsiblePerson]]](cachedAmendVariationRenewalResponse,
      ResponsiblePerson.key, responsiblePeopleSection(viewResponse.responsiblePeopleSection))

    val cachedRenewal = saveRenewalData(viewResponse, cachedResponsiblePerson)

    cacheConnector.saveAll(cachedRenewal)
  }

  private def viewResponseSection(viewResponse: ViewResponse) = {
    Some(businessMatchingSection(viewResponse.businessMatchingSection))
  }

  private def eabSection(viewResponse: ViewResponse) = {
    Some(viewResponse.eabSection.copy(hasAccepted = true))
  }

  private def aboutSection(viewResponse: ViewResponse) = {
    viewResponse.aboutTheBusinessSection.copy(hasAccepted = true)
  }

  private def activitySection(viewResponse: ViewResponse) = {
    Some(viewResponse.businessActivitiesSection.copy(hasAccepted = true))
  }

  private def tcspSection(viewResponse: ViewResponse) = {
    Some(viewResponse.tcspSection.copy(hasAccepted = true))
  }

  private def aspSection(viewResponse: ViewResponse) = {
    Some(viewResponse.aspSection.copy(hasAccepted = true))
  }

  private def msbSection(viewResponse: ViewResponse) = {
    Some(viewResponse.msbSection.copy(hasAccepted = true))
  }

  private def hvdSection(viewResponse: ViewResponse) = {
    Some(viewResponse.hvdSection.copy(hasAccepted = true))
  }

  private def supervisionSection(viewResponse: ViewResponse) = {
    Some(viewResponse.supervisionSection.copy(hasAccepted = true))
  }

  private def businessMatchingSection(viewResponse: BusinessMatching): BusinessMatching = {
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

  private def responsiblePeopleSection(viewResponse: Option[Seq[ResponsiblePerson]]): Option[Seq[ResponsiblePerson]] =
    Some(viewResponse.fold(Seq.empty[ResponsiblePerson])(_.map(rp => rp.copy(hasAccepted = true))))

  private def tradingPremisesSection(viewResponse: Option[Seq[TradingPremises]]): Option[Seq[TradingPremises]] =
    Some(viewResponse.fold(Seq.empty[TradingPremises])(_.map(tp => tp.copy(hasAccepted = true))))

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
        cacheConnector.upsert[Renewal](cacheMap, Renewal.key, renewal)
      }
      case _ => cacheMap
    }
  }

  private def fixAddress(model: AboutTheBusiness) = (model.correspondenceAddress, model.altCorrespondenceAddress) match {
    case (Some(_), None) => model.copy(altCorrespondenceAddress = Some(true), hasAccepted = true)
    case (None, None) => model.copy(altCorrespondenceAddress = Some(false), hasAccepted = true)
    case _ => model
  }
}

object LandingService extends LandingService {
  // $COVERAGE-OFF$
  override private[services] lazy val cacheConnector = DataCacheConnector
  override private[services] lazy val keyStore = KeystoreConnector
  override private[services] lazy val desConnector = AmlsConnector
  override private[services] lazy val statusService = StatusService
  override private[services] lazy val businessMatchingConnector = Play.current.injector.instanceOf[BusinessMatchingConnector]
  // $COVERAGE-ON$
}
