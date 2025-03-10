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

import connectors.{AmlsConnector, DataCacheConnector}
import models.businessmatching.BusinessMatching
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthorisedRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RecoverActivitiesService @Inject() (amlsConnector: AmlsConnector, dataCacheConnector: DataCacheConnector)
    extends Logging {

  def recover(
    request: AuthorisedRequest[_]
  )(implicit messages: Messages, hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    request.amlsRefNumber match {
      case Some(refNumber) =>
        for {
          desResponse              <- amlsConnector.view(refNumber, request.accountTypeId)
          cachedBusinessMatching   <- dataCacheConnector.fetch[BusinessMatching](request.credId, BusinessMatching.key)
          refreshedBusinessMatching =
            cachedBusinessMatching.map(_.copy(activities = desResponse.businessMatchingSection.activities))
          _                        <-
            dataCacheConnector.save[BusinessMatching](request.credId, BusinessMatching.key, refreshedBusinessMatching)
        } yield {
          val listOfBusinessTypes = refreshedBusinessMatching.prefixedAlphabeticalBusinessTypes()
          listOfBusinessTypes match {
            case Some(list) if list.nonEmpty =>
              true
            case Some(_)                     =>
              logger.warn(
                "[RecoverActivitiesService][recover] - Empty list of business types returned by DES, unable to fix record"
              )
              false
            case _                           =>
              logger.warn(
                "[RecoverActivitiesService][recover] - No BusinessActivities section returned by DES, unable to fix record"
              )
              false
          }
        }
      case None            =>
        logger.warn("[RecoverActivitiesService][recover] - No AMLS ref number was found in request")
        Future.successful(false)
    }
}
