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
import models.responsiblepeople.ResponsiblePeople
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{RepeatingSection, StatusConstants}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResponsiblePeopleService @Inject()(val dataCacheConnector: DataCacheConnector) extends RepeatingSection {

  def getAll(implicit hc: HeaderCarrier, ac: AuthContext, ec: ExecutionContext) =
    dataCacheConnector.fetch[Seq[ResponsiblePeople]](ResponsiblePeople.key) map {
      _.getOrElse(Seq.empty)
    }

  def updateFitAndProperFlag(responsiblePeople: Seq[ResponsiblePeople], indices: Set[Int]): Seq[ResponsiblePeople] =
    responsiblePeople.zipWithIndex.map { case (rp, index) =>
      val updated = if (indices contains index) {
        rp.hasAlreadyPassedFitAndProper(Some(true))
      } else {
        rp.hasAlreadyPassedFitAndProper(Some(false))
      }

      updated.copy(hasAccepted = rp.hasAccepted)
    }
}

object ResponsiblePeopleService {

  def isActive(person: ResponsiblePeople) = !person.status.contains(StatusConstants.Deleted) && person.isComplete

  implicit class ResponsiblePeopleListHelpers(people: Seq[(ResponsiblePeople, Int)]) {
    def exceptInactive = people filter {
      case (person, _) if isActive(person) => true
      case _ => false
    }
  }

}
