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

package controllers.changeofficer

import cats.data.OptionT
import connectors.DataCacheConnector
import models.changeofficer.NewOfficer
import models.responsiblepeople.{NominatedOfficer, ResponsiblePerson}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.StatusConstants

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object Helpers {

  def getNominatedOfficerName()(implicit authContext: AuthContext,
                                headerCarrier: HeaderCarrier,
                                dataCacheConnector: DataCacheConnector,
                                f: cats.Monad[Future]): OptionT[Future, String] = {
    for {
      people <- OptionT(dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key))
      (nominatedOfficer, _) <- OptionT.fromOption[Future](getOfficer(ResponsiblePerson.filterWithIndex(people)))
      name <- OptionT.fromOption[Future](nominatedOfficer.personName)
    } yield {
      name.fullName
    }
  }

  def getOfficer(people: Seq[(ResponsiblePerson, Int)]): Option[(ResponsiblePerson, Int)] = {
    people.map {
      case (person, index) => (person, index + 1)
    } find {
      case (person, _) => person.positions.fold(false)(_.positions contains NominatedOfficer)
    }
  }

  def getNominatedOfficerWithIndex()(implicit authContext: AuthContext,
                                headerCarrier: HeaderCarrier,
                                dataCacheConnector: DataCacheConnector,
                                f: cats.Monad[Future]): OptionT[Future, (ResponsiblePerson, Int)] = {
    for {
      people <- OptionT(dataCacheConnector.fetch[Seq[ResponsiblePerson]](ResponsiblePerson.key))
      nominatedOfficer <- OptionT.fromOption[Future](getOfficer(people.zipWithIndex))
    } yield nominatedOfficer
  }

}
