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

import com.fasterxml.jackson.core.JsonParseException
import config.AppConfig
import connectors.{AmlsConnector, DataCacheConnector}
import exceptions.{DuplicateSubscriptionException, NoEnrolmentException}
import javax.inject.Inject
import models._
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOfficeUK}
import models.asp.Asp
import models.bankdetails.{BankDetails, NoBankAccountUsed}
import models.businessactivities.BusinessActivities
import models.businessmatching.{BusinessMatching, BusinessActivities => BusinessSevices, MoneyServiceBusiness => MSB}
import models.declaration.AddPerson
import models.estateagentbusiness.EstateAgentBusiness
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Conversions._
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePerson
import models.responsiblepeople.ResponsiblePerson.FilterUtils
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.tradingpremises.TradingPremises.FilterUtils
import play.api.http.Status.UNPROCESSABLE_ENTITY
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.StatusConstants

import scala.concurrent.{ExecutionContext, Future}

class SubmissionService @Inject()
(
  val cacheConnector: DataCacheConnector,
  val ggService: GovernmentGatewayService,
  val authEnrolmentsService: AuthEnrolmentsService,
  val amlsConnector: AmlsConnector,
  config: AppConfig
) extends DataCacheService {

  private def enrol(safeId: String, amlsRegistrationNumber: String, postcode: String)
                   (implicit hc: HeaderCarrier, ec: ExecutionContext, ac: AuthContext): Future[_] =
    if (config.enrolmentStoreToggle) {
      authEnrolmentsService.enrol(amlsRegistrationNumber, postcode)
    } else {
      ggService.enrol(amlsRegistrationNumber, safeId, postcode)
    }

  private def errorResponse(response: Upstream4xxResponse): Option[SubscriptionErrorResponse] = response match {
    case e if e.upstreamResponseCode == UNPROCESSABLE_ENTITY =>
      try {
        Json.parse(e.getMessage).asOpt[SubscriptionErrorResponse]
      } catch {
        case _: JsonParseException => None
      }
    case _ => None
  }

  def subscribe
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[SubscriptionResponse] = {
    (for {
      cache <- getCache
      safeId <- safeId(cache)
      request <- Future.successful(createSubscriptionRequest(cache))
      subscription <- amlsConnector.subscribe(request, safeId)
      _ <- saveResponse(subscription, SubscriptionResponse.key)
      _ <- enrol(safeId, subscription.amlsRefNo, request.aboutTheBusinessSection.fold("")(_.registeredOffice match {
        case Some(o: RegisteredOfficeUK) => o.postCode
        case _ => ""
      }))
    } yield subscription) recoverWith {
      case e: Upstream4xxResponse if e.upstreamResponseCode == UNPROCESSABLE_ENTITY =>
        Future.failed(SubscriptionErrorResponse.from(e).fold[Throwable](e)(r => DuplicateSubscriptionException(r.message)))
    }
  }

  private def createSubscriptionRequest
  (cache: CacheMap)
  (implicit
   ac: AuthContext,
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): SubscriptionRequest = {

    def filteredResponsiblePeople = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key).map(_.filterEmpty)

    def filteredTradingPremises = cache.getEntry[Seq[TradingPremises]](TradingPremises.key).map(_.filterEmpty)

    SubscriptionRequest(
      businessMatchingSection = cache.getEntry[BusinessMatching](BusinessMatching.key),
      eabSection = cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key),
      tradingPremisesSection = filteredTradingPremises,
      aboutTheBusinessSection = cache.getEntry[AboutTheBusiness](AboutTheBusiness.key),
      bankDetailsSection = bankDetailsExceptDeleted(cache.getEntry[Seq[BankDetails]](BankDetails.key)),
      aboutYouSection = cache.getEntry[AddPerson](AddPerson.key),
      businessActivitiesSection = cache.getEntry[BusinessActivities](BusinessActivities.key),
      responsiblePeopleSection = filteredResponsiblePeople,
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
        regNo.getOrElse(throw NoEnrolmentException("[SubmissionService][update] - No enrolment"))
      )
      _ <- saveResponse(amendment, AmendVariationRenewalResponse.key)
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
        regNo.getOrElse(throw NoEnrolmentException("[SubmissionService][variation] - No enrolment"))
      )
      _ <- saveResponse(amendment, AmendVariationRenewalResponse.key)

    } yield amendment
  }

  def renewal(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      response <- amlsConnector.renewal(
        createSubscriptionRequest(cache).withRenewalData(renewal),
        regNo.getOrElse(throw NoEnrolmentException("[SubmissionService][renewal] - No enrolment"))
      )
      _ <- saveResponse(response, AmendVariationRenewalResponse.key)
    } yield response
  }

  def renewalAmendment(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {
    for {
      cache <- getCache
      regNo <- authEnrolmentsService.amlsRegistrationNumber
      response <- amlsConnector.renewalAmendment(
        createSubscriptionRequest(cache).withRenewalData(renewal),
        regNo.getOrElse(throw NoEnrolmentException("[SubmissionService][renewalAmendment] - No enrolment"))
      )
      _ <- saveResponse(response, AmendVariationRenewalResponse.key, isRenewalAmendment = true)
    } yield response
  }

  private def saveResponse[T](response: T, key: String, isRenewalAmendment: Boolean = false)
                             (implicit ac: AuthContext, hc: HeaderCarrier, ex: ExecutionContext, fmt: Format[T]) = for {
    _ <- cacheConnector.save[T](key, response)
    c <- cacheConnector.save[SubmissionRequestStatus](SubmissionRequestStatus.key, SubmissionRequestStatus(true, isRenewalAmendment))
  } yield c

  private def safeId(cache: CacheMap): Future[String] = {
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      rd <- bm.reviewDetails
    } yield rd.safeId) match {
      case Some(a) => Future.successful(a)
      case _ => Future.failed(new Exception("No SafeID value available"))
    }
  }

  def bankDetailsExceptDeleted(bankDetails: Option[Seq[BankDetails]]): Option[Seq[BankDetails]] = {
    bankDetails match {
      case Some(bankAccts) =>

        val bankDtls = bankAccts.filterNot(
          x => x.status.contains(StatusConstants.Deleted)
            || x.bankAccountType.isEmpty
            || x.bankAccountType.contains(NoBankAccountUsed))

        if (bankDtls.nonEmpty) {
          Some(bankDtls)
        } else {
          Some(Seq.empty)
        }

      case _ => Some(Seq.empty)
    }
  }

}
