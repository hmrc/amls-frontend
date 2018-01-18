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
import models.ViewResponse
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
import models.responsiblepeople.ResponsiblePeople
import models.status.RenewalSubmitted
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.withdrawal.WithdrawalStatus
import play.api.mvc.Request
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

trait LandingService {

  private[services] val cacheConnector: DataCacheConnector
  private[services] val keyStore: KeystoreConnector
  private[services] val desConnector: AmlsConnector
  private[services] val statusService: StatusService
  private[services] val businessMatchingConnector: BusinessMatchingConnector

  @deprecated("fetch the cacheMap itself instead", "")
  def hasSavedForm
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   ac: AuthContext
  ): Future[Boolean] =
    cacheConnector.fetchAll map {
      case Some(_) => true
      case None => false
    }

  def cacheMap
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   ac: AuthContext
  ): Future[Option[CacheMap]] =
    cacheConnector.fetchAll

  def remove
  (implicit
   hc: HeaderCarrier
  ): Future[HttpResponse] = {
    cacheConnector.remove(BusinessMatching.key)
  }

  private def saveRenewalData(viewResponse: ViewResponse, cacheMap: CacheMap)(implicit
                                                                              authContext: AuthContext,
                                                                              hc: HeaderCarrier,
                                                                              ec: ExecutionContext
  ): Future[CacheMap] = {

    import models.businessactivities.{InvolvedInOther => BAInvolvedInOther}
    import models.hvd.{PercentageOfCashPaymentOver15000 => HvdRPercentageOfCashPaymentOver15000, ReceiveCashPayments => HvdReceiveCashPayments}
    import models.moneyservicebusiness.{WhichCurrencies => MsbWhichCurrencies}

    statusService.getStatus flatMap {
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
        cacheConnector.save[Renewal](Renewal.key, renewal)
      }
      case _ => Future.successful(cacheMap)

    }

  }

  private def fixAddress(model: AboutTheBusiness) = (model.correspondenceAddress, model.altCorrespondenceAddress) match {
    case (Some(_), None) => model.copy(altCorrespondenceAddress = Some(true), hasAccepted = true)
    case (None, None) => model.copy(altCorrespondenceAddress = Some(false), hasAccepted = true)
    case _ => model
  }

  def setAlCorrespondenceAddressWithRegNo(amlsRefNumber: String, cacheMap: Option[CacheMap])
                                         (implicit
                                          authContext: AuthContext,
                                          hc: HeaderCarrier,
                                          ec: ExecutionContext
                                         ): Future[CacheMap] = {
    val cachedModel = for {
      cache <- OptionT.fromOption[Future](cacheMap)
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
                  (implicit authContext: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[CacheMap] = for {
    viewResponse <- desConnector.view(amlsRefNumber)
    withdrawalStatus <- cacheConnector.fetch[WithdrawalStatus](WithdrawalStatus.key)
    _ <- cacheConnector.remove(authContext.user.oid)
    _ <- cacheConnector.save[WithdrawalStatus](WithdrawalStatus.key, withdrawalStatus getOrElse WithdrawalStatus(false))
    _ <- cacheConnector.save[Option[ViewResponse]](ViewResponse.key, Some(viewResponse))
    _ <- cacheConnector.save[BusinessMatching](BusinessMatching.key, Some(businessMatchingSection(viewResponse.businessMatchingSection)))
    _ <- cacheConnector.save[Option[EstateAgentBusiness]](EstateAgentBusiness.key, Some(viewResponse.eabSection.copy(hasAccepted = true)))
    _ <- cacheConnector.save[Option[Seq[TradingPremises]]](TradingPremises.key, tradingPremisesSection(viewResponse.tradingPremisesSection))
    _ <- cacheConnector.save[AboutTheBusiness](AboutTheBusiness.key, viewResponse.aboutTheBusinessSection.copy(hasAccepted = true))
    _ <- cacheConnector.save[Seq[BankDetails]](BankDetails.key, writeEmptyBankDetails(viewResponse.bankDetailsSection))
    _ <- cacheConnector.save[AddPerson](AddPerson.key, viewResponse.aboutYouSection)
    _ <- cacheConnector.save[BusinessActivities](BusinessActivities.key, Some(viewResponse.businessActivitiesSection.copy(hasAccepted = true)))
    _ <- cacheConnector.save[Option[Tcsp]](Tcsp.key, Some(viewResponse.tcspSection.copy(hasAccepted = true)))
    _ <- cacheConnector.save[Option[Asp]](Asp.key, Some(viewResponse.aspSection.copy(hasAccepted = true)))
    _ <- cacheConnector.save[Option[MoneyServiceBusiness]](MoneyServiceBusiness.key, Some(viewResponse.msbSection.copy(hasAccepted = true)))
    _ <- cacheConnector.save[Option[Hvd]](Hvd.key, Some(viewResponse.hvdSection.copy(hasAccepted = true)))
    _ <- cacheConnector.save[Option[Supervision]](Supervision.key, Some(viewResponse.supervisionSection.copy(hasAccepted = true)))
    cache1 <- cacheConnector.save[Option[Seq[ResponsiblePeople]]](ResponsiblePeople.key, responsiblePeopleSection(viewResponse.responsiblePeopleSection))
    cache2 <- saveRenewalData(viewResponse, cache1)
  } yield cache2

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

  def responsiblePeopleSection(viewResponse: Option[Seq[ResponsiblePeople]]): Option[Seq[ResponsiblePeople]] =
    Some(viewResponse.fold(Seq.empty[ResponsiblePeople])(_.map(rp => rp.copy(hasAccepted = true))))

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

  /* TODO: Consider if there's a good way to stop
   * this from just overwriting whatever is in Business Matching,
   * shouldn't be a problem as this should only happen when someone
   * first comes into the Application from Business Customer FE
   */
  def updateReviewDetails
  (reviewDetails: ReviewDetails)
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   ac: AuthContext
  ): Future[CacheMap] = {
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
