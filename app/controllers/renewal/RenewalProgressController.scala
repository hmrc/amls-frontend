/*
 * Copyright 2021 HM Revenue & Customs
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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import models.registrationprogress.Completed
import models.responsiblepeople.ResponsiblePerson
import models.status.{ReadyForRenewal, RenewalSubmitted}
import play.api.mvc.MessagesControllerComponents
import services.businessmatching.BusinessMatchingService
import services.{ProgressService, RenewalService, SectionsProvider, StatusService}
import utils.{AuthAction, ControllerHelper}
import views.html.renewal.renewal_progress

import scala.concurrent.Future

@Singleton
class RenewalProgressController @Inject()(val authAction: AuthAction,
                                          val ds: CommonPlayDependencies,
                                          val dataCacheConnector: DataCacheConnector,
                                          val progressService: ProgressService,
                                          val sectionsProvider: SectionsProvider,
                                          val renewals: RenewalService,
                                          val businessMatchingService: BusinessMatchingService,
                                          val statusService: StatusService,
                                          val cc: MessagesControllerComponents,
                                          renewal_progress: renewal_progress) extends AmlsBaseController(ds, cc) {

  def get = authAction.async {
      implicit request =>
        val statusInfo = statusService.getDetailedStatus(request.amlsRefNumber, request.accountTypeId, request.credId)
        val result = statusInfo map {
          case (r: ReadyForRenewal, _) => {
            for {
              renewalSection <- OptionT.liftF(renewals.getSection(request.credId))
              cache <- OptionT(dataCacheConnector.fetchAll(request.credId))
              responsiblePeople <- OptionT.fromOption[Future](cache.getEntry[Seq[ResponsiblePerson]](ResponsiblePerson.key))
              businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
            } yield {
              val businessName = businessMatching.reviewDetails.map(r => r.businessName).getOrElse("")
              val activities = businessMatching.activities.fold(Seq.empty[String])(_.businessActivities.map(_.getMessage()).toSeq)
              val variationSections = sectionsProvider.sections(cache).filter(_.name != BusinessMatching.messageKey)
              val canSubmit = renewals.canSubmit(renewalSection, variationSections)
              val msbOrTcspExists = ControllerHelper.isMSBSelected(Some(businessMatching)) ||
                ControllerHelper.isTCSPSelected(Some(businessMatching))
              val hasCompleteNominatedOfficer = ControllerHelper.hasCompleteNominatedOfficer(Option(responsiblePeople))
              val nominatedOfficerName = ControllerHelper.completeNominatedOfficerTitleName(Option(responsiblePeople))

              Ok(renewal_progress(variationSections, businessName, activities, canSubmit, msbOrTcspExists, r, renewalSection.status == Completed, hasCompleteNominatedOfficer, nominatedOfficerName))
            }
          }
          case (r:RenewalSubmitted, _) => OptionT.fromOption[Future](Some(Redirect(controllers.routes.RegistrationProgressController.get)))
        }
        result.flatMap(_.getOrElse(InternalServerError("Cannot get business matching or renewal date")))
  }

  def post() = authAction.async {
      implicit request =>
        progressService.getSubmitRedirect(request.amlsRefNumber, request.accountTypeId, request.credId) map {
          case Some(url) => Redirect(url)
          case _ => InternalServerError("Could not get data for redirect")
        }
  }

}
