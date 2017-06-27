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

package utils

import controllers.tradingpremises.routes
import models.businessmatching._
import models.responsiblepeople.{NominatedOfficer, NonUKResidence, ResponsiblePeople, UKResidence}
import models.status.{NotCompleted, SubmissionReady, SubmissionReadyForReview}
import models.tradingpremises.TradingPremises
import play.api.Play.current
import play.api.i18n.Messages
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request, Results}
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

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

  def getMsbServices(matching: Option[BusinessMatching]): Option[Set[MsbService]] = {
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

  def isTCSPSelected(bm: Option[BusinessMatching]): Boolean = {
    bm match {
      case Some(matching) => matching.activities.foldLeft(false) { (x, y) =>
        y.businessActivities.contains(TrustAndCompanyServices)
      }
    }
  }

  //For repeating section
  def allowedToEdit(edit: Boolean)(implicit statusService: StatusService, hc: HeaderCarrier, auth: AuthContext): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionReady | NotCompleted => true
      case _ => !edit
    }
  }

  def allowedToEdit(implicit statusService: StatusService, hc: HeaderCarrier, auth: AuthContext): Future[Boolean] = {
    statusService.getStatus map {
      case SubmissionReady | NotCompleted | SubmissionReadyForReview  => true
      case _ => false
    }
  }

  def hasNominatedOfficer(eventualMaybePeoples: Future[Option[Seq[ResponsiblePeople]]]): Future[Boolean] = {
    eventualMaybePeoples map {
      case Some(rps) =>  rps.filter(!_.status.contains(StatusConstants.Deleted)).exists(_.positions.fold(false)(_.positions.contains(NominatedOfficer)))
      case _ =>  false
    }
  }
  def hasNonUkResident(rp: Option[Seq[ResponsiblePeople]]): Boolean = {
    rp match {
      case Some(rps) => rps.exists(_.personResidenceType.fold(false)(_.isUKResidence match {
        case NonUKResidence => true
        case _ => false
      }))
      case _ =>  false
    }
  }


  def rpTitleName(rp:Option[ResponsiblePeople]):String = rp.fold("")(_.personName.fold("")(_.titleName))

  def notFoundView(implicit request: Request[_]) = {
    views.html.error(Messages("error.not-found.title"),
      Messages("error.not-found.heading"),
      Messages("error.not-found.message"))
  }
}
