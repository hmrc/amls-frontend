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

package services.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.ResponsiblePerson
import utils.StatusConstants

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourResponsiblePeopleService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit
  ec: ExecutionContext
) {

  def completeAndIncompleteRP(
    credId: String
  ): Future[Option[(Seq[(ResponsiblePerson, Int)], Seq[(ResponsiblePerson, Int)])]] = {
    val completeIncompleteRp: Future[Option[Seq[(ResponsiblePerson, Int)]]] =
      dataCacheConnector.fetch[Seq[ResponsiblePerson]](credId, ResponsiblePerson.key).map { optResponsiblePeople =>
        optResponsiblePeople.map { rp =>
          rp.zipWithIndex.reverse
            .filterNot(_._1.status.contains(StatusConstants.Deleted))
            .filterNot(_._1 == ResponsiblePerson())
        }
      }

    completeIncompleteRp.map(_.map(_.partition(_._1.isComplete)))
  }
}
