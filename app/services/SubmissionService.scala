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

import config.ApplicationConfig
import connectors.{AmlsConnector, BusinessMatchingConnector, DataCacheConnector}
import exceptions.{DuplicateSubscriptionException, NoEnrolmentException}
import models._
import models.amp.Amp
import models.asp.Asp
import models.bankdetails.BankAccountType.NoBankAccountUsed
import models.bankdetails.BankDetails
import models.businessactivities.BusinessActivities
import models.businessdetails.{BusinessDetails, RegisteredOfficeUK}
import models.businessmatching.BusinessMatching
import models.declaration.AddPerson
import models.eab.Eab
import models.hvd.Hvd
import models.moneyservicebusiness.MoneyServiceBusiness
import models.renewal.Conversions._
import models.renewal.Renewal
import models.responsiblepeople.ResponsiblePerson
import models.supervision.Supervision
import models.tcsp.Tcsp
import models.tradingpremises.TradingPremises
import models.tradingpremises.TradingPremises.FilterUtils
import play.api.http.Status.UNPROCESSABLE_ENTITY
import play.api.libs.json.Format
import services.cache.Cache
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.StatusConstants

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionService @Inject()(val cacheConnector: DataCacheConnector,
                                  val ggService: GovernmentGatewayService,
                                  val authEnrolmentsService: AuthEnrolmentsService,
                                  val amlsConnector: AmlsConnector,
                                  config: ApplicationConfig,
                                  val businessMatchingConnector: BusinessMatchingConnector) extends DataCacheService {

  private def enrol(safeId: String, amlsRegistrationNumber: String, postcode: String, groupId: Option[String], credId: String)
                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[_] =

    authEnrolmentsService.enrol(amlsRegistrationNumber, postcode, groupId, credId)

  def subscribe(credId: String, accountTypeId: (String, String), groupId: Option[String])
               (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[SubscriptionResponse] = {
    (for {
      cache <- getCache(credId)
      safeId <- safeId(cache)
      request <- Future.successful(createSubscriptionRequest(cache))
      subscription <- amlsConnector.subscribe(request, safeId, accountTypeId)
      _ <- saveResponse(credId, subscription, SubscriptionResponse.key)
      _ <- enrol(safeId, subscription.amlsRefNo, request.businessDetailsSection.fold("")(_.registeredOffice match {
        case Some(o: RegisteredOfficeUK) => o.postCode
        case _ => ""
      }), groupId, credId)
    } yield subscription) recoverWith {
      case e: UpstreamErrorResponse if e.statusCode == UNPROCESSABLE_ENTITY =>
        Future.failed(SubscriptionErrorResponse.from(e).fold[Throwable](e)(r => DuplicateSubscriptionException(r.message)))
    }
  }

  private def createSubscriptionRequest(cache: Cache): SubscriptionRequest = {

    def filteredResponsiblePeople = cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key).map(_.filterEmpty)

    def filteredTradingPremises = cache.getEntry[Seq[TradingPremises]](TradingPremises.key).map(_.filterEmpty)

    SubscriptionRequest(
      businessMatchingSection = cache.getEntry[BusinessMatching](BusinessMatching.key),
      eabSection = cache.getEntry[Eab](Eab.key),
      tradingPremisesSection = filteredTradingPremises,
      businessDetailsSection = cache.getEntry[BusinessDetails](BusinessDetails.key),
      bankDetailsSection = bankDetailsExceptDeleted(cache.getEntry[Seq[BankDetails]](BankDetails.key)),
      aboutYouSection = cache.getEntry[AddPerson](AddPerson.key),
      businessActivitiesSection = cache.getEntry[BusinessActivities](BusinessActivities.key),
      responsiblePeopleSection = filteredResponsiblePeople,
      tcspSection = cache.getEntry[Tcsp](Tcsp.key),
      aspSection = cache.getEntry[Asp](Asp.key),
      msbSection = cache.getEntry[MoneyServiceBusiness](MoneyServiceBusiness.key),
      hvdSection = cache.getEntry[Hvd](Hvd.key),
      ampSection = cache.getEntry[Amp](Amp.key),
      supervisionSection = cache.getEntry[Supervision](Supervision.key)
    )
  }

  def update(credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
            (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[AmendVariationRenewalResponse] = {

    for {
      cache <- getCache(credId)
      amendment <- amlsConnector.update(
        createSubscriptionRequest(cache),
        amlsRegistrationNumber.getOrElse(throw NoEnrolmentException("[SubmissionService][update] - No enrolment")), accountTypeId)
      _ <- saveResponse(credId, amendment, AmendVariationRenewalResponse.key)
    } yield amendment
  }

  def variation(credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String))
               (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[AmendVariationRenewalResponse] = {

    for {
      cache <- getCache(credId)
      amendment <- amlsConnector.variation(
        createSubscriptionRequest(cache),
        amlsRegistrationNumber.getOrElse(throw NoEnrolmentException("[SubmissionService][variation] - No enrolment")), accountTypeId)
      _ <- saveResponse(credId, amendment, AmendVariationRenewalResponse.key)

    } yield amendment
  }

  def renewal(credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String), renewal: Renewal)
             (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {

    for {
      cache <- getCache(credId)
      response <- amlsConnector.renewal(
        createSubscriptionRequest(cache).withRenewalData(renewal),
        amlsRegistrationNumber.getOrElse(throw NoEnrolmentException("[SubmissionService][renewal] - No enrolment")), accountTypeId)
      _ <- saveResponse(credId, response, AmendVariationRenewalResponse.key)
    } yield response
  }

  def renewalAmendment(credId: String, amlsRegistrationNumber: Option[String], accountTypeId: (String, String), renewal: Renewal)
                      (implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[SubmissionResponse] = {

    for {
      cache <- getCache(credId)
      response <- amlsConnector.renewalAmendment(
        createSubscriptionRequest(cache).withRenewalData(renewal),
        amlsRegistrationNumber.getOrElse(throw NoEnrolmentException("[SubmissionService][renewalAmendment] - No enrolment")), accountTypeId)
      _ <- saveResponse(credId, response, AmendVariationRenewalResponse.key, isRenewalAmendment = true)
    } yield response
  }

  private def saveResponse[T](credId: String, response: T, key: String, isRenewalAmendment: Boolean = false)
                             (implicit ex: ExecutionContext, fmt: Format[T]): Future[Cache] = {

    for {
      _ <- cacheConnector.save[T](credId, key, response)
      c <- cacheConnector.save[SubmissionRequestStatus](credId, SubmissionRequestStatus.key, SubmissionRequestStatus(hasSubmitted = true, Some(isRenewalAmendment)))
    } yield c
  }

  private def safeId(cache: Cache)(implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[String] =
    (for {
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
      rd <- bm.reviewDetails
    } yield rd.safeId) match {
      case Some(a) =>
        if (a.trim.isEmpty) {
          businessMatchingConnector.getReviewDetails map {
            case Some(details) => details.safeId
            case _ => throw new Exception("No safe id from business customer service")
          }
        } else {
          Future.successful(a)
        }
      case _ => Future.failed(new Exception("No SafeID value available"))
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
