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

package utils

import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching._
import models.businessmatching.updateservice.ServiceChangeRegister
import models.renewal.CustomersOutsideUK
import models.responsiblepeople.{NonUKResidence, ResponsiblePerson}
import models.status._
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import services.StatusService
import services.businessmatching.ServiceFlow
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ControllerHelper {

  def getBusinessType(matching: Option[BusinessMatching]): Option[BusinessType] = {
    matching flatMap { bm =>
      bm.reviewDetails match {
        case Some(review) => review.businessType
        case _ => None
      }
    }
  }

  def getMsbServices(matching: Option[BusinessMatching]): Option[Set[BusinessMatchingMsbService]] = {
    matching flatMap { bm =>
        bm.msbServices match {
          case Some(service) => Some(service.msbServices)
          case _ => None
        }
      }
  }

  def getBusinessActivity(matching: Option[BusinessMatching]): Option[BusinessActivities] = {
    matching match {
      case Some(data) => data.activities
      case None => None
    }
  }

  def isMSBSelected(bm: Option[BusinessMatching]): Boolean = {
    bm match {
      case Some(matching) => matching.activities.foldLeft(false) { (x, y) =>
        y.businessActivities.contains(MoneyServiceBusiness)
      }
      case None => false
    }
  }

  def hasCustomersOutsideUK(customersOutsideUK: Option[CustomersOutsideUK]): Boolean = {
    customersOutsideUK.flatMap {
      case CustomersOutsideUK(Some(country)) => Some(country)
      case _ => None
    }.isDefined
  }

  def isTCSPSelected(bm: Option[BusinessMatching]): Boolean = {
    bm match {
      case Some(matching) => matching.activities.foldLeft(false) { (x, y) =>
        y.businessActivities.contains(TrustAndCompanyServices)
      }
    }
  }

  //For repeating section


  def allowedToEdit(implicit statusService: StatusService, hc: HeaderCarrier, auth: AuthContext): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview  => true
      case _ => false
    }
  }

  def allowedToEdit(activity: BusinessActivity, msbSubSector: Option[BusinessMatchingMsbService] = None)
                   (implicit statusService: StatusService, cacheConnector: DataCacheConnector, hc: HeaderCarrier, auth: AuthContext, serviceFlow: ServiceFlow): Future[Boolean] = for {
    status <- statusService.getStatus
    isNewActivity <- serviceFlow.isNewActivity(activity)
    changeRegister <- cacheConnector.fetch[ServiceChangeRegister](ServiceChangeRegister.key)
  } yield (status, isNewActivity, changeRegister, msbSubSector) match {
    case (SubmissionDecisionApproved | ReadyForRenewal(_), _, Some(c), Some(m)) if c.addedSubSectors.fold(false)(_.contains(m)) => true
    case (_, true, _, _) => true
    case (SubmissionReady | NotCompleted | SubmissionReadyForReview, false, _, _) => true
    case _ => false
  }

  def hasNominatedOfficer(eventualMaybePeoples: Future[Option[Seq[ResponsiblePerson]]]): Future[Boolean] = {
    eventualMaybePeoples map {
      case Some(rps) => ResponsiblePerson.filter(rps).exists(_.isNominatedOfficer)
      case _ => false
    }
  }

  def getNominatedOfficer(responsiblePeople: Seq[ResponsiblePerson]): Option[ResponsiblePerson] = {
    ResponsiblePerson.filter(responsiblePeople).filter(_.isNominatedOfficer) match {
      case rps@_::_ => Some(rps.head)
      case _ => None
    }
  }

  def nominatedOfficerTitleName(responsiblePeople: Option[Seq[ResponsiblePerson]]): Option[String] = {
    responsiblePeople map { rps =>
      rpTitleName(getNominatedOfficer(rps))
    }
  }

  def hasNonUkResident(rp: Option[Seq[ResponsiblePerson]]): Boolean = {
    rp match {
      case Some(rps) => rps.exists(_.personResidenceType.fold(false)(_.isUKResidence match {
        case NonUKResidence => true
        case _ => false
      }))
      case _ =>  false
    }
  }

  def rpTitleName(rp:Option[ResponsiblePerson]):String = rp.fold("")(_.personName.fold("")(_.titleName))

  def notFoundView(implicit request: Request[_]) = {
    views.html.error(Messages("error.not-found.title"),
      Messages("error.not-found.heading"),
      Messages("error.not-found.message"))
  }
}
