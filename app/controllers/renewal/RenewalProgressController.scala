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

package controllers.renewal

import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.{BusinessActivity, BusinessMatching}
import models.registrationprogress.Section
import models.status.{ReadyForRenewal, RenewalSubmitted}
import play.api.i18n.MessagesApi
import services.businessmatching.BusinessMatchingService
import services.{ProgressService, RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.registrationamendment.registration_amendment
import views.html.renewal.renewal_progress

import scala.concurrent.Future

@Singleton
class RenewalProgressController @Inject()
(
  val authConnector: AuthConnector,
  val dataCacheConnector: DataCacheConnector,
  val progressService: ProgressService,
  val messages: MessagesApi,
  val renewals: RenewalService,
  val businessMatchingService: BusinessMatchingService,
  val statusService :StatusService
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
          val block = for {
            renewalSection <- OptionT.liftF(renewals.getSection)
            cache <- OptionT(dataCacheConnector.fetchAll)
            statusInfo <- OptionT.liftF(statusService.getDetailedStatus)
          } yield {
            val variationSections = progressService.sections(cache).filter(_.name != BusinessMatching.messageKey)
            val businessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)
            val canSubmit = renewals.canSubmit(renewalSection, variationSections)

            val result = statusInfo match {
              case (r:ReadyForRenewal, _) => {
                val msbOrTcspExists = ControllerHelper.isMSBSelected(businessMatching) || ControllerHelper.isTCSPSelected(businessMatching)

                Future.successful(Some(Ok(renewal_progress(variationSections, canSubmit, msbOrTcspExists, r))))
              }
              case (r:RenewalSubmitted, _) => handleRenewalSubmitted(cache, variationSections, canSubmit, businessMatching).value
              case _ => throw new Exception("Cannot get renewal date")
            }
            
            result
          }
          block getOrElse InternalServerError("Cannot get business matching or renewal date")
  }

  private def handleRenewalSubmitted(cache: CacheMap,
                                     variationSections: Seq[Section],
                                     canSubmit: Boolean,
                                     businessMatching: BusinessMatching) = for {
    reviewDetails <- OptionT.fromOption[Future](businessMatching.reviewDetails)
    newActivities <- businessMatchingService.getAdditionalBusinessActivities orElse OptionT.some(Set.empty[BusinessActivity])

  } yield {
    val newSections = progressService.sectionsFromBusinessActivities(newActivities, businessMatching.msbServices)(cache).toSeq
    val activities = businessMatching.activities.fold(Seq.empty[String])(_.businessActivities.map(_.getMessage).toSeq)

    Ok(registration_amendment(
      variationSections,
      canSubmit,
      reviewDetails.businessAddress,
      activities,
      false,
      Some(newSections)
    ))
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        progressService.getSubmitRedirect map {
          case Some(url) => Redirect(url)
          case _ => InternalServerError("Could not get data for redirect")
        }
  }

}
