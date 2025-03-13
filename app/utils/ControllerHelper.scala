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

package utils

import connectors.DataCacheConnector
import models.businessactivities.{BusinessActivities => BA}
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.renewal.CustomersOutsideUK
import models.responsiblepeople.ResponsiblePerson.filter
import models.responsiblepeople.{NonUKResidence, ResponsiblePerson}
import models.status._
import models.supervision.{AnotherBody, AnotherBodyNo, AnotherBodyYes, Supervision}
import play.api.i18n.Messages
import play.api.mvc.Request
import play.twirl.api.HtmlFormat
import services.StatusService
import services.businessmatching.ServiceFlow
import services.cache.Cache
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object ControllerHelper {

  def hasIncompleteResponsiblePerson(rps: Option[Seq[ResponsiblePerson]]): Boolean =
    rps.exists((data: Seq[ResponsiblePerson]) => filter(data).exists(_.isComplete equals false))

  def getBusinessType(matching: Option[BusinessMatching]): Option[BusinessType] =
    matching flatMap { bm =>
      bm.reviewDetails match {
        case Some(review) => review.businessType
        case _            => None
      }
    }

  def getMsbServices(matching: Option[BusinessMatching]): Option[Set[BusinessMatchingMsbService]] =
    matching flatMap { bm =>
      bm.msbServices match {
        case Some(service) => Some(service.msbServices)
        case _             => None
      }
    }

  def getBusinessActivity(matching: Option[BusinessMatching]): Option[BusinessActivities] =
    matching match {
      case Some(data) => data.activities
      case None       => None
    }

  def isMSBSelected(bm: Option[BusinessMatching]): Boolean =
    bm match {
      case Some(matching) =>
        matching.activities.foldLeft(false) { (x, y) =>
          y.businessActivities.contains(MoneyServiceBusiness)
        }
      case None           => false
    }

  def hasCustomersOutsideUK(customersOutsideUK: Option[CustomersOutsideUK]): Boolean =
    customersOutsideUK.flatMap {
      case CustomersOutsideUK(Some(country)) => Some(country)
      case _                                 => None
    }.isDefined

  def isTCSPSelected(bm: Option[BusinessMatching]): Boolean =
    bm match {
      case Some(matching) =>
        matching.activities.foldLeft(false) { (x, y) =>
          y.businessActivities.contains(TrustAndCompanyServices)
        }
      case _              => false
    }

  def isAccountancyServicesSelected(bm: BusinessMatching): Boolean =
    bm.activities match {
      case Some(activities) => activities.businessActivities.contains(AccountancyServices)
      case _                => false
    }

  // For repeating section
  def allowedToEdit(amlsRegistrationNo: Option[String], accountTypeId: (String, String), credId: String)(implicit
    statusService: StatusService,
    hc: HeaderCarrier,
    ec: ExecutionContext,
    messages: Messages
  ): Future[Boolean] =
    statusService.getStatus(amlsRegistrationNo, accountTypeId, credId) map {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview => true
      case _                                                         => false
    }

  def allowedToEdit(
    activity: BusinessActivity,
    msbSubSector: Option[BusinessMatchingMsbService] = None,
    amlsRegistrationNo: Option[String],
    accountTypeId: (String, String),
    credId: String
  )(implicit
    statusService: StatusService,
    cacheConnector: DataCacheConnector,
    hc: HeaderCarrier,
    serviceFlow: ServiceFlow,
    ec: ExecutionContext,
    messages: Messages
  ): Future[Boolean] = for {
    status         <- statusService.getStatus(amlsRegistrationNo, accountTypeId, credId)
    isNewActivity  <- serviceFlow.isNewActivity(credId, activity)
    changeRegister <- cacheConnector.fetch[ServiceChangeRegister](credId, ServiceChangeRegister.key)
  } yield (status, isNewActivity, changeRegister, msbSubSector) match {
    case (SubmissionDecisionApproved | ReadyForRenewal(_), _, Some(c), Some(m))
        if c.addedSubSectors.fold(false)(_.contains(m)) =>
      true
    case (_, true, _, _)                                                          => true
    case (SubmissionReady | NotCompleted | SubmissionReadyForReview, false, _, _) => true
    case _                                                                        => false
  }

  def hasNominatedOfficer(
    eventualMaybePeoples: Future[Option[Seq[ResponsiblePerson]]]
  )(implicit ec: ExecutionContext): Future[Boolean] =
    eventualMaybePeoples map {
      case Some(rps) => ResponsiblePerson.filter(rps).exists(_.isNominatedOfficer)
      case _         => false
    }

  def hasCompleteNominatedOfficer(responsiblePeople: Option[Seq[ResponsiblePerson]]): Boolean =
    responsiblePeople match {
      case Some(rps) => ResponsiblePerson.filter(rps).exists(rp => rp.isComplete && rp.isNominatedOfficer)
      case _         => false
    }

  def getNominatedOfficer(responsiblePeople: Seq[ResponsiblePerson]): Option[ResponsiblePerson] =
    ResponsiblePerson.filter(responsiblePeople).find(_.isNominatedOfficer)

  def getCompleteNominatedOfficer(responsiblePeople: Seq[ResponsiblePerson]): Option[ResponsiblePerson] =
    ResponsiblePerson.filter(responsiblePeople).find(rp => rp.isComplete && rp.isNominatedOfficer)

  def nominatedOfficerTitleName(responsiblePeople: Option[Seq[ResponsiblePerson]]): Option[String] =
    responsiblePeople map { rps =>
      rpTitleName(getNominatedOfficer(rps))
    }

  def completeNominatedOfficerTitleName(responsiblePeople: Option[Seq[ResponsiblePerson]]): Option[String] =
    responsiblePeople map { rps =>
      rpTitleName(getCompleteNominatedOfficer(rps))
    }

  def hasNonUkResident(rp: Option[Seq[ResponsiblePerson]]): Boolean =
    rp match {
      case Some(rps) =>
        rps.exists(_.personResidenceType.fold(false)(_.isUKResidence match {
          case NonUKResidence => true
          case _              => false
        }))
      case _         => false
    }

  def rpTitleName(rp: Option[ResponsiblePerson]): String = rp.fold("")(_.personName.fold("")(_.titleName))

  def notFoundView(implicit
    request: Request[_],
    messages: Messages,
    error: views.html.ErrorView
  ): HtmlFormat.Appendable =
    error(Messages("error.not-found.title"), Messages("error.not-found.heading"), Messages("error.not-found.message"))

  def anotherBodyComplete(supervision: Supervision): Option[(Boolean, Boolean)] = (for {
    anotherBody <- supervision.anotherBody
  } yield anotherBody) match {
    case Some(AnotherBodyNo) => Option((true, false))
    case Some(body)          => Option((body.asInstanceOf[AnotherBodyYes].isComplete(), true))
    case None                => None
  }

  def isAnotherBodyYes(abCompleteAndYes: Option[(Boolean, Boolean)]): Boolean =
    abCompleteAndYes match {
      case Some(yes) if yes._2 => true
      case _                   => false
    }

  def isAnotherBodyComplete(abCompleteAndYes: Option[(Boolean, Boolean)]): Boolean =
    abCompleteAndYes match {
      case Some(complete) if complete._1 => true
      case _                             => false
    }

  def accountantName(ba: Option[BA]): String = (for {
    businessActivities  <- ba
    whoIsYourAccountant <- businessActivities.whoIsYourAccountant
    names               <- whoIsYourAccountant.names
  } yield names.accountantsName).getOrElse("")

  def supervisionComplete(cache: Cache): Boolean = cache.getEntry[Supervision](Supervision.key) match {
    case Some(supervision) => supervision.isComplete
    case _                 => false
  }

  def isAbComplete(anotherBody: AnotherBody): Boolean = anotherBody match {
    case AnotherBodyYes(_, Some(_), Some(_), Some(_)) => true
    case AnotherBodyNo                                => true
    case _                                            => false
  }
}
