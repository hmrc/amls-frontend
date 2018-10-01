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

import connectors.DataCacheConnector
import javax.inject.{Inject, Singleton}

import models.businessmatching.updateservice.ResponsiblePeopleFitAndProper
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResponsiblePeopleService @Inject()(val dataCacheConnector: DataCacheConnector) extends RepeatingSection {

  def getAll(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext) =
    dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key) map {
      _.getOrElse(Seq.empty)
    }

  def updateFitAndProperFlag(responsiblePeople: Seq[ResponsiblePerson], indices: Set[Int]): Seq[ResponsiblePerson] =
    responsiblePeople.zipWithIndex.map { case (rp, index) =>
      val updated = if (indices contains index) {
        rp.approvalFlags(ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)))
      } else {
        rp.approvalFlags(ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)))
      }

      updated.copy(hasAccepted = rp.hasAccepted)
    }
}

object ResponsiblePeopleService {

  def isActive(person: ResponsiblePerson) = !person.status.contains(StatusConstants.Deleted) && person.isComplete

  implicit class ResponsiblePeopleZipListHelpers(people: Seq[(ResponsiblePerson, Int)]) {
    def exceptInactive = people filter {
      case (person, _) if isActive(person) => true
      case _ => false
    }
  }

  implicit class ResponsiblePeopleListHelpers(people: Seq[ResponsiblePerson]) {
    def exceptInactive = people filter {
      case person if isActive(person) => true
      case _ => false
    }
  }
}
