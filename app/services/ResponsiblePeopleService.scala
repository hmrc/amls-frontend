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

import connectors.DataCacheConnector
import models.responsiblepeople.ResponsiblePerson
import utils.{RepeatingSection, StatusConstants}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResponsiblePeopleService @Inject() (val dataCacheConnector: DataCacheConnector) extends RepeatingSection {

  def getAll(credId: String)(implicit ec: ExecutionContext): Future[Seq[ResponsiblePerson]] =
    dataCacheConnector.fetch[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key) map {
      _.getOrElse(Seq.empty)
    }

  def updateFitAndProperFlag(
    responsiblePeople: Seq[ResponsiblePerson],
    indices: Set[Int],
    setApprovalFlag: Boolean
  ): Seq[ResponsiblePerson] =
    responsiblePeople.zipWithIndex.map { case (rp, index) =>
      val updated = if (setApprovalFlag) {
        rp.approvalFlags(
          rp.approvalFlags.copy(
            hasAlreadyPassedFitAndProper = Some(indices contains index),
            hasAlreadyPaidApprovalCheck = Some(indices contains index)
          )
        )
      } else {
        rp.approvalFlags(rp.approvalFlags.copy(hasAlreadyPassedFitAndProper = Some(indices contains index)))
      }
      updated.copy(hasAccepted = rp.hasAccepted)
    }
}

object ResponsiblePeopleService {

  def isActive(person: ResponsiblePerson): Boolean =
    !person.status.contains(StatusConstants.Deleted) && person.isComplete

  def nonDeleted(person: ResponsiblePerson): Boolean = !person.status.contains(StatusConstants.Deleted)

  implicit class ResponsiblePeopleZipListHelpers(people: Seq[(ResponsiblePerson, Int)]) {
    def exceptInactive: Seq[(ResponsiblePerson, Int)] = people filter {
      case (person, _) if isActive(person) => true
      case _                               => false
    }

    def exceptDeleted: Seq[(ResponsiblePerson, Int)] = people filter {
      case (person, _) if nonDeleted(person) => true
      case _                                 => false
    }
  }

  implicit class ResponsiblePeopleListHelpers(people: Seq[ResponsiblePerson]) {
    def exceptInactive: Seq[ResponsiblePerson] = people filter {
      case person if isActive(person) => true
      case _                          => false
    }
  }
}
