/*
 * Copyright 2024 HM Revenue & Customs
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
import com.google.inject.Inject
import connectors.{AmlsConnector, BusinessMatchingConnector, DataCacheConnector}
import models._
import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.{BusinessActivities, ExpectedAMLSTurnover, ExpectedBusinessTurnover}
import models.businesscustomer.ReviewDetails
import models.businessdetails.BusinessDetails
import models.businessmatching.{BusinessMatching, BusinessActivities => BMActivities}
import models.declaration.AddPerson
import models.eab.Eab
import models.hvd.{Hvd, ReceiveCashPayments}
import models.moneyservicebusiness.{MostTransactions => _, SendTheLargestAmountsOfMoney => _, WhichCurrencies => _, _}
import models.renewal._
import models.responsiblepeople.ResponsiblePerson
import models.status.RenewalSubmitted
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import play.api.i18n.Messages
import services.cache.Cache
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class LandingService @Inject()(val cacheConnector: DataCacheConnector,
                               val amlsConnector: AmlsConnector,
                               val statusService: StatusService,
                               val businessMatchingConnector: BusinessMatchingConnector){

  def cacheMap(credId: String): Future[Option[Cache]] = cacheConnector.fetchAll(credId)

  def initialiseGetWithAmendments(credId: String)(implicit ec: ExecutionContext): Future[Option[Cache]] = {
    cacheConnector.fetchAll(credId).map { optCacheMap =>
      if (optCacheMap.isDefined) {
        val cacheMap = optCacheMap.head

        if (!cacheMap.data.contains(TradingPremises.key)) {
          cacheConnector.save[Seq[TradingPremises]](credId, TradingPremises.key, Seq.empty[TradingPremises])
        }

        if (!cacheMap.data.contains(ResponsiblePerson.key)) {
          cacheConnector.save[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key, Seq.empty[ResponsiblePerson])
        }
      }

      optCacheMap
    }
  }

  def setAltCorrespondenceAddress(amlsRefNumber: String, maybeCache: Option[Cache], accountTypeId: (String, String), credId: String)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Cache] = {
    val cachedModel = for {
      cache <- OptionT.fromOption[Future](maybeCache)
      entry <- OptionT.fromOption[Future](cache.getEntry[BusinessDetails](BusinessDetails.key))
    } yield entry

    lazy val etmpModel = OptionT.liftF(amlsConnector.view(amlsRefNumber, accountTypeId) map { v => v.businessDetailsSection })

    (for {
      businessDetails <- cachedModel orElse etmpModel
      cacheMap <- OptionT.liftF(cacheConnector.save[BusinessDetails](credId, BusinessDetails.key, fixAddress(businessDetails)))
    } yield cacheMap) getOrElse (throw new Exception("Unable to update alt correspondence address"))
  }

  def setAltCorrespondenceAddress(businessDetails: BusinessDetails, credId: String): Future[Cache] =
    cacheConnector.save[BusinessDetails](credId, BusinessDetails.key, fixAddress(businessDetails))

  def refreshCache(amlsRefNumber: String, credId: String, accountTypeId: (String, String))
                  (implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Cache] =
    for {
      viewResponse           <- amlsConnector.view(amlsRefNumber, accountTypeId)
      subscriptionResponse   <- cacheConnector.fetch[SubscriptionResponse](credId, SubscriptionResponse.key).recover { case _ => None }
      amendVariationResponse <- cacheConnector.fetch[AmendVariationRenewalResponse](credId, AmendVariationRenewalResponse.key) recover { case _ => None }
      _                      <- cacheConnector.remove(credId) // MUST clear cash first to remove stale data and reload from API5
      appCache               <- cacheConnector.fetchAllWithDefault(credId)
      refreshedCache         <- {
        upsertCacheEntries(appCache, viewResponse, subscriptionResponse, amendVariationResponse, amlsRefNumber, accountTypeId, credId)
      }
    } yield refreshedCache

  def writeEmptyBankDetails(bankDetailsSeq: Seq[BankDetails]): Seq[BankDetails] = {
    val empty = Seq.empty[BankDetails]
    bankDetailsSeq match {
      case `empty` => Seq(BankDetails(None, None, None, hasChanged = false, refreshedFromServer = true, None, hasAccepted = true))
      case _ =>
        bankDetailsSeq map {
          bank => bank.copy(hasAccepted = true)
        }
    }
  }

  def reviewDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ReviewDetails]] =
    businessMatchingConnector.getReviewDetails map {
      case Some(details) => Some(ReviewDetails.convert(details))
      case _ => None
    }

  /* Consider if there's a good way to stop
   * this from just overwriting whatever is in Business Matching,
   * shouldn't be a problem as this should only happen when someone
   * first comes into the Application from Business Customer FE
   */
  def updateReviewDetails(reviewDetails: ReviewDetails, credId: String): Future[Cache] = {
    val bm = BusinessMatching(reviewDetails = Some(reviewDetails))
    cacheConnector.save[BusinessMatching](credId, BusinessMatching.key, bm)
  }

  /* **********
   * Privates *
   ************/
  private def upsertCacheEntries(appCache: Cache, viewResponse: ViewResponse, subscriptionResponse: Option[SubscriptionResponse],
                                 amendVariationResponse: Option[AmendVariationRenewalResponse], amlsRegistrationNo: String,
                                 accountTypeId: (String, String), cacheId: String)
                                (implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Cache] = {

    val cachedViewResponse = cacheConnector.upsertNewAuth[Option[ViewResponse]](appCache, ViewResponse.key, Some(viewResponse))

    val cachedBusinessMatching = cacheConnector.upsertNewAuth[BusinessMatching](cachedViewResponse, BusinessMatching.key,
      viewResponseSection(viewResponse))

    val cachedEstateAgentBusiness = cacheConnector.upsertNewAuth[Option[Eab]](cachedBusinessMatching,
      Eab.key, eabSection(viewResponse))

    val cachedTradingPremises = cacheConnector.upsertNewAuth[Option[Seq[TradingPremises]]](cachedEstateAgentBusiness, TradingPremises.key,
      tradingPremisesSection(viewResponse.tradingPremisesSection))

    val cachedBusinessDetails = cacheConnector.upsertNewAuth[BusinessDetails](cachedTradingPremises, BusinessDetails.key, aboutSection(viewResponse))

    val cachedBankDetails = cacheConnector.upsertNewAuth[Seq[BankDetails]](
      cachedBusinessDetails, BankDetails.key, writeEmptyBankDetails(viewResponse.bankDetailsSection))

    val cachedAddPerson = cacheConnector.upsertNewAuth[AddPerson](cachedBankDetails, AddPerson.key, viewResponse.aboutYouSection)

    val cachedBusinessActivities = cacheConnector.upsertNewAuth[BusinessActivities](cachedAddPerson, BusinessActivities.key, activitySection(viewResponse))

    val cachedTcsp = cacheConnector.upsertNewAuth[Option[Tcsp]](cachedBusinessActivities, Tcsp.key, tcspSection(viewResponse))

    val cachedAsp = cacheConnector.upsertNewAuth[Option[Asp]](cachedTcsp, Asp.key, aspSection(viewResponse))

    val cachedMoneyServiceBusiness = cacheConnector.upsertNewAuth[Option[MoneyServiceBusiness]](cachedAsp, MoneyServiceBusiness.key, msbSection(viewResponse))

    val cachedHvd = cacheConnector.upsertNewAuth[Option[Hvd]](cachedMoneyServiceBusiness, Hvd.key, hvdSection(viewResponse))

    val cachedAmp = cacheConnector.upsertNewAuth[Option[Amp]](cachedHvd, Amp.key, ampSection(viewResponse))

    val cachedSupervision = cacheConnector.upsertNewAuth[Option[Supervision]](cachedAmp, Supervision.key, supervisionSection(viewResponse))

    val cachedSubscriptionResponse = cacheConnector.upsertNewAuth[Option[SubscriptionResponse]](cachedSupervision,
      SubscriptionResponse.key, subscriptionResponse)

    val cachedAmendVariationRenewalResponse = cacheConnector.upsertNewAuth[Option[AmendVariationRenewalResponse]](cachedSubscriptionResponse,
      AmendVariationRenewalResponse.key, amendVariationResponse)

    val cachedResponsiblePerson = cacheConnector.upsertNewAuth[Option[Seq[ResponsiblePerson]]](cachedAmendVariationRenewalResponse,
      ResponsiblePerson.key, responsiblePeopleSection(viewResponse.responsiblePeopleSection))

    val cachedRenewal = saveRenewalData(viewResponse, cachedResponsiblePerson, amlsRegistrationNo, accountTypeId, cacheId)

    cacheConnector.saveAll(cacheId, cachedRenewal)
  }

  private def viewResponseSection(viewResponse: ViewResponse): Option[BusinessMatching] = {
    Some(businessMatchingSection(viewResponse.businessMatchingSection))
  }

  private def eabSection(viewResponse: ViewResponse): Option[Eab] = {
    if (viewResponse.eabSection.isDefined) {
      viewResponse.eabSection.map(eab => eab.copy(hasAccepted = true))
    } else {
      None
    }
  }

  private def aboutSection(viewResponse: ViewResponse): BusinessDetails = {
    viewResponse.businessDetailsSection.copy(hasAccepted = true)
  }

  private def activitySection(viewResponse: ViewResponse): Option[BusinessActivities] = {
    Some(viewResponse.businessActivitiesSection.copy(hasAccepted = true))
  }

  private def tcspSection(viewResponse: ViewResponse): Option[Tcsp] = {
    if (viewResponse.tcspSection.tcspTypes.nonEmpty) {
      Some(viewResponse.tcspSection.copy(hasAccepted = true))
    } else {
      None
    }
  }

  private def aspSection(viewResponse: ViewResponse): Option[Asp] = {
    if (viewResponse.aspSection.services.nonEmpty) {
      Some(viewResponse.aspSection.copy(hasAccepted = true))
    } else {
      None
    }
  }

  private def msbSection(viewResponse: ViewResponse):Option[MoneyServiceBusiness] = {
    if(viewResponse.msbSection.throughput.nonEmpty) {
      Some(viewResponse.msbSection.copy(hasAccepted = true))
    } else {
      None
    }
  }

  private def ampSection(viewResponse: ViewResponse): Option[Amp] = {
    viewResponse.ampSection.map(amp => amp.copy(hasAccepted = true))
  }

  private def hvdSection(viewResponse: ViewResponse): Option[Hvd] = {
    if (viewResponse.hvdSection.products.nonEmpty) {
      Some(viewResponse.hvdSection.copy(hasAccepted = true))
    } else {
      None
    }
  }

  private def supervisionSection(viewResponse: ViewResponse): Option[Supervision] = {
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

  private def saveRenewalData(viewResponse: ViewResponse, cache: Cache, amlsRegistrationNo: String, accountTypeId: (String, String), cacheId: String)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Cache] = {

    import models.businessactivities.{InvolvedInOther => BAInvolvedInOther}
    import models.hvd.{PercentageOfCashPaymentOver15000 => HvdRPercentageOfCashPaymentOver15000}
    import models.moneyservicebusiness.{WhichCurrencies => MsbWhichCurrencies}

    for {
      renewalStatus <- statusService.getStatus(Option(amlsRegistrationNo), accountTypeId, cacheId)
    } yield renewalStatus match {
      case RenewalSubmitted(_) =>
        val renewal = Some(Renewal(
          involvedInOtherActivities = viewResponse.businessActivitiesSection.involvedInOther.map(i => BAInvolvedInOther.convert(i)),
          businessTurnover = viewResponse.businessActivitiesSection.expectedBusinessTurnover.map(data => ExpectedBusinessTurnover.convert(data)),
          turnover = viewResponse.businessActivitiesSection.expectedAMLSTurnover.map(data => ExpectedAMLSTurnover.convert(data)),
          ampTurnover = viewResponse.ampSection map {amp => Amp.convert(amp)},
          customersOutsideUK = viewResponse.businessActivitiesSection.customersOutsideUK.map(c => CustomersOutsideUK(c.countries)),

          percentageOfCashPaymentOver15000 = viewResponse.hvdSection.fold[Option[PercentageOfCashPaymentOver15000]](None)
            (_.percentageOfCashPaymentOver15000.map(p => HvdRPercentageOfCashPaymentOver15000.convert(p))),

          receiveCashPayments = viewResponse.hvdSection.fold[Option[CashPayments]](None) { hvd =>
            hvd.receiveCashPayments.map(r => ReceiveCashPayments.convert(hvd))
          },

          totalThroughput = viewResponse.msbSection.fold[Option[TotalThroughput]](None)(_.throughput.map(t => ExpectedThroughput.convert(t))),

          whichCurrencies = viewResponse.msbSection.fold[Option[WhichCurrencies]](None)(_.whichCurrencies.map(c => MsbWhichCurrencies.convert(c))),

          transactionsInLast12Months = viewResponse.msbSection.fold[Option[TransactionsInLast12Months]](None)
            (_.transactionsInNext12Months.map(t => TransactionsInLast12Months(t.txnAmount))),

          sendTheLargestAmountsOfMoney = viewResponse.msbSection.fold[Option[SendTheLargestAmountsOfMoney]](None)
            (_.sendTheLargestAmountsOfMoney.map(s => SendTheLargestAmountsOfMoney(s.countries))),

          mostTransactions = viewResponse.msbSection.fold[Option[MostTransactions]](None)
            (_.mostTransactions.map(m => MostTransactions(m.countries))),

          ceTransactionsInLast12Months = viewResponse.msbSection.fold[Option[CETransactionsInLast12Months]](None)
            (_.ceTransactionsInNext12Months.map(t => CETransactionsInLast12Months(t.ceTransaction)))
        ))
        cacheConnector.upsertNewAuth[Renewal](cache, Renewal.key, renewal)
      case _ => cache
    }
  }

  private def fixAddress(model: BusinessDetails): BusinessDetails = (model.correspondenceAddress, model.altCorrespondenceAddress) match {
    case (Some(_), None) => model.copy(altCorrespondenceAddress = Some(true), hasAccepted = true)
    case (None, None) => model.copy(altCorrespondenceAddress = Some(false), hasAccepted = true)
    case _ => model
  }
}
