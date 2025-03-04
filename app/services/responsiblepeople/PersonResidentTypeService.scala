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

import cats.data.OptionT
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.{NonUKResidence, PersonResidenceType, ResponsiblePerson, UKResidence}
import services.cache.Cache
import utils.RepeatingSection

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonResidentTypeService @Inject() (val dataCacheConnector: DataCacheConnector)(implicit ec: ExecutionContext)
    extends RepeatingSection {

  def getResponsiblePerson(credId: String, index: Int): Future[Option[ResponsiblePerson]] =
    getData[ResponsiblePerson](credId, index)

  def getCache(data: PersonResidenceType, credId: String, index: Int): OptionT[Future, Cache] =
    OptionT(
      fetchAllAndUpdateStrict[ResponsiblePerson](credId, index) { (_, rp) =>
        val nationality    = rp.personResidenceType.fold[Option[Country]](None)(x => x.nationality)
        val countryOfBirth = rp.personResidenceType.fold[Option[Country]](None)(x => x.countryOfBirth)
        val updatedData    = data.copy(countryOfBirth = countryOfBirth, nationality = nationality)
        data.isUKResidence match {
          case UKResidence(_) => rp.personResidenceType(updatedData).copy(ukPassport = None, nonUKPassport = None)
          case NonUKResidence => rp.personResidenceType(updatedData)
        }
      }
    )
}
