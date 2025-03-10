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
import models.businesscustomer.Address
import models.businessdetails._
import models.businessmatching.BusinessMatching
import services.cache.Cache

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmRegisteredOfficeService @Inject() (val dataCache: DataCacheConnector)(implicit ec: ExecutionContext) {

  def hasRegisteredAddress(credId: String): Future[Option[Boolean]] =
    dataCache.fetch[BusinessDetails](credId, BusinessDetails.key).map { optBusinessDetails =>
      optBusinessDetails.map(_.registeredOffice.isDefined)
    }

  def getAddress(credId: String): Future[Option[Address]] =
    dataCache.fetch[BusinessMatching](credId, BusinessMatching.key).map { optBusinessMatching =>
      optBusinessMatching.flatMap(_.reviewDetails.map(_.businessAddress))
    }

  def updateRegisteredOfficeAddress(credId: String, data: ConfirmRegisteredOffice): Future[Option[RegisteredOffice]] = {
    def updateFromCacheMap(optCache: Option[Cache]): Option[Future[RegisteredOffice]] = optCache.flatMap { cache =>
      cache.getEntry[BusinessMatching](BusinessMatching.key).flatMap { bm =>
        cache.getEntry[BusinessDetails](BusinessDetails.key).flatMap { bd =>
          if (data.isRegOfficeOrMainPlaceOfBusiness) {
            updateBMAddress(bm) map { address =>
              dataCache
                .save[BusinessDetails](credId, BusinessDetails.key, bd.registeredOffice(address))
                .map(_ => address)
            }
          } else {
            None
          }
        }
      }
    }

    for {
      cacheOpt <- dataCache.fetchAll(credId)
      updated  <- updateFromCacheMap(cacheOpt).sequence
    } yield updated
  }

  private def updateBMAddress(bm: BusinessMatching): Option[RegisteredOffice] =
    bm.reviewDetails.fold[Option[RegisteredOffice]](None)(rd =>
      if (rd.businessAddress.postcode.isDefined) {
        Some(
          RegisteredOfficeUK(
            rd.businessAddress.line_1,
            rd.businessAddress.line_2,
            rd.businessAddress.line_3,
            rd.businessAddress.line_4,
            rd.businessAddress.postcode.getOrElse("")
          )
        )
      } else {
        Some(
          RegisteredOfficeNonUK(
            rd.businessAddress.line_1,
            rd.businessAddress.line_2,
            rd.businessAddress.line_3,
            rd.businessAddress.line_4,
            rd.businessAddress.country
          )
        )
      }
    )
}
