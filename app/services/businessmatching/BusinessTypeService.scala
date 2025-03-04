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

package services.businessmatching

import cats.implicits._
import connectors.DataCacheConnector
import models.businessmatching.{BusinessMatching, BusinessType}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessTypeService @Inject() (
  val cacheConnector: DataCacheConnector
) {

  def getBusinessType(credId: String)(implicit ec: ExecutionContext): Future[Option[BusinessType]] =
    cacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key) map { bmOpt =>
      for {
        bm            <- bmOpt
        reviewDetails <- bm.reviewDetails
        businessType  <- reviewDetails.businessType
      } yield businessType
    }

  def updateBusinessType(credId: String, businessType: BusinessType)(implicit
    ec: ExecutionContext
  ): Future[Option[BusinessType]] = {
    cacheConnector.fetch[BusinessMatching](credId, BusinessMatching.key) map { bm =>
      bm.reviewDetails map { rd =>
        cacheConnector.save[BusinessMatching](
          credId,
          BusinessMatching.key,
          bm.reviewDetails(rd.copy(businessType = Some(businessType)))
        ) map (_ => businessType)
      }
    }
  } flatMap (_.sequence)
}
