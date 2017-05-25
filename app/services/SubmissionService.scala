/*
 * Copyright 2017 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.{AmlsConnector, DataCacheConnector, GovernmentGatewayConnector}
import exceptions.NoEnrolmentException
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessActivities => BusinessSevices, BusinessMatching, MoneyServiceBusiness => MSB}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Conversions._
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePeople
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.{AmendVariationRenewalResponse, SubmissionResponse, SubscriptionRequest, SubscriptionResponse}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.StatusConstants

import scala.concurrent.{ExecutionContext, Future}

//noinspection ScalaStyle
trait SubmissionService extends DataCacheService {

  private[services] def cacheConnector: DataCacheConnector

  private[services] def amlsConnector: AmlsConnector

  private[services] def ggService: GovernmentGatewayService

  private[services] def authEnrolmentsService: AuthEnrolmentsService


  def subscribe
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[SubscriptionResponse] = {
    for {
      cache <- getCache
      safeId <- safeId(cache)
      subscription <- amlsConnector.subscribe(createSubscriptionRequest(cache), safeId)
      _ <- cacheConnector.save[SubscriptionResponse](SubscriptionResponse.key, subscription)
      _ <- ggService.enrol(
        safeId = safeId,
        mlrRefNo = subscription.amlsRefNo
      )
    } yield subscription
  }

  private def createSubscriptionRequest
  (cache: CacheMap)
  (implicit
   ac: AuthContext,
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): SubscriptionRequest = {
    SubscriptionRequest(
      businessMatchingSection = cache.getEntry[BusinessMatching](BusinessMatching.key),
      eabSection = cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key),
      tradingPremisesSection = cache.getEntry[Seq[TradingPremises]](TradingPremises.key),
      aboutTheBusinessSection = cache.getEntry[AboutTheBusiness](AboutTheBusiness.key),
      bankDetailsSection = bankDetailsExceptDeleted(cache.getEntry[Seq[BankDetails]](BankDetails.key)),
      aboutYouSection = cache.getEntry[AddPerson](AddPerson.key),
      businessActivitiesSection = cache.getEntry[BusinessActivities](BusinessActivities.key),
      responsiblePeopleSection = cache.getEntry[Seq[ResponsiblePeople]](ResponsiblePeople.key),
      tcspSection = cache.getEntry[Tcsp](Tcsp.key),
      aspSection = cache.getEntry[Asp](Asp.key),
      msbSection = cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key),
      hvdSection = cache.getEntry[Hvd](Hvd.key),
      supervisionSection = cache.getEntry[Supervision](Supervision.key)
    )
  }

  def update
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[AmendVariationRenewalResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      amendment <- amlsConnector.update(
        createSubscriptionRequest(cache),
        regNo.getOrElse(throw new NoEnrolmentException("[SubmissionService][update] - No enrolment"))
      )
      _ <- cacheConnector.save[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key, amendment)
    } yield amendment
  }

  def variation
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[AmendVariationRenewalResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      amendment <- amlsConnector.variation(
        createSubscriptionRequest(cache),
        regNo.getOrElse(throw new NoEnrolmentException("[SubmissionService][variation] - No enrolment"))
      )
      _ <- cacheConnector.save[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key, amendment)
    } yield amendment
  }

  def renewal(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      response <- amlsConnector.renewal(
        createSubscriptionRequest(cache).withRenewalData(renewal),
        regNo.getOrElse(throw new NoEnrolmentException("[SubmissionService][renewal] - No enrolment"))
      )
      _ <- cacheConnector.save[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key, response)
    } yield response
  }

  def renewalAmendment(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      response <- amlsConnector.renewalAmendment(
        createSubscriptionRequest(cache).withRenewalData(renewal),
        regNo.getOrElse(throw new NoEnrolmentException("[SubmissionService][renewalAmendment] - No enrolment"))
      )
      _ <- cacheConnector.save[AmendVariationRenewalResponse](AmendVariationRenewalResponse.key, response)
    } yield response
  }

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

  def bankDetailsExceptDeleted(bankDetails: Option[Seq[BankDetails]]): Option[Seq[BankDetails]] = {
    bankDetails match {
      case Some(bankAccts) => {
        val bankDtls = bankAccts.filterNot(x => x.status.contains(StatusConstants.Deleted) || x.bankAccountType.isEmpty)
        bankDtls.nonEmpty match {
          case true => Some(bankDtls)
          case false => Some(Seq.empty)
        }
      }
      case _ => Some(Seq.empty)
    }
  }

}

object SubmissionService extends SubmissionService {

  // $COVERAGE-OFF$
  object MockGGService extends GovernmentGatewayService {

    import play.api.http.Status.OK

    override private[services] def ggConnector: GovernmentGatewayConnector = GovernmentGatewayConnector

    override def enrol
    (mlrRefNo: String, safeId: String)
    (implicit
     hc: HeaderCarrier,
     ec: ExecutionContext
    ): Future[HttpResponse] = Future.successful(HttpResponse(OK))
  }

  override private[services] val cacheConnector = DataCacheConnector
  override private[services] val amlsConnector = AmlsConnector
  override private[services] val authEnrolmentsService = AuthEnrolmentsService
  override private[services] val ggService = {
    if (ApplicationConfig.enrolmentToggle) {
      GovernmentGatewayService
    } else {
      MockGGService
    }
  }
}
