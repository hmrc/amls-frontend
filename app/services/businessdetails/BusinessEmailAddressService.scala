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

package services.businessdetails

import cats.implicits._
import connectors.DataCacheConnector
import models.businessdetails.{BusinessDetails, ContactingYou, ContactingYouEmail}
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessEmailAddressService @Inject() (val dataCache: DataCacheConnector)(implicit ec: ExecutionContext) {

  def getEmailAddress(credId: String): Future[Option[String]] =
    dataCache.fetch[BusinessDetails](credId, BusinessDetails.key).map(_.flatMap(_.contactingYou.flatMap(_.email)))

  def updateEmailAddress(credId: String, data: ContactingYouEmail): Future[Option[Cache]] =
    dataCache.fetch[BusinessDetails](credId, BusinessDetails.key).map {
      _ map { businessDetails =>
        val updatedBusinessDetails = businessDetails.contactingYou(
          businessDetails.contactingYou.fold(ContactingYou(email = Some(data.email))) { cy =>
            cy.copy(email = Some(data.email))
          }
        )
        dataCache.save[BusinessDetails](credId, BusinessDetails.key, updatedBusinessDetails)
      }
    } flatMap (_.sequence)
}
