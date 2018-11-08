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

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import javax.inject.{Inject, Singleton}
import models.businessmatching.BusinessMatching
import models.registrationprogress.Completed
import models.status.{ReadyForRenewal, RenewalSubmitted}
import play.api.i18n.MessagesApi
import services.businessmatching.BusinessMatchingService
import services.{ProgressService, RenewalService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
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
  val statusService: StatusService
) extends BaseController {

  def get = Authorised.async {
    implicit authContext =>
      implicit request =>
        val statusInfo = statusService.getDetailedStatus

        val result = statusInfo map {
          case (r: ReadyForRenewal, _) => {
            for {
              renewalSection <- OptionT.liftF(renewals.getSection)
              cache <- OptionT(dataCacheConnector.fetchAll)
              businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
            } yield {

              val variationSections = progressService.sections(cache).filter(_.name != BusinessMatching.messageKey)
              val canSubmit = renewals.canSubmit(renewalSection, variationSections)
              val msbOrTcspExists = ControllerHelper.isMSBSelected(Some(businessMatching)) ||
                ControllerHelper.isTCSPSelected(Some(businessMatching))
              Ok(renewal_progress(variationSections, canSubmit, msbOrTcspExists, r, renewalSection.status == Completed))
            }
          }
          case (r:RenewalSubmitted, _) => OptionT.fromOption[Future](Some(Redirect(controllers.routes.RegistrationProgressController.get)))
        }
        result.flatMap(_.getOrElse(InternalServerError("Cannot get business matching or renewal date")))
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
