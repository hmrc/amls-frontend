/*
 * Copyright 2017 HM Revenue & Customs
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
import models.businessmatching.BusinessMatching
import models.registrationprogress.Completed
import models.status.ReadyForRenewal
import play.api.i18n.MessagesApi
import services.{ProgressService, RenewalService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.renewal.renewal_progress

import scala.concurrent.Future

@Singleton
class RenewalProgressController @Inject()
(
  val authConnector: AuthConnector,
  dataCacheConnector: DataCacheConnector,
  progressService: ProgressService,
  messages: MessagesApi,
  renewals: RenewalService,
  statusService :StatusService
) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        renewals.getSection flatMap { renewalSection =>

          val canSubmit = renewalSection.status == Completed

          val block = for {
            cache <- OptionT(dataCacheConnector.fetchAll)
            statusInfo <- OptionT.liftF(statusService.getDetailedStatus)
          } yield {
            val variationSections = progressService.sections(cache)
            val businessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)
            val msbOrTcspExists = ControllerHelper.isMSBSelected(businessMatching) || ControllerHelper.isTCSPSelected(businessMatching)

            statusInfo match {
              case (ReadyForRenewal(renewalDate), _) => Ok(renewal_progress(renewalSection, variationSections, canSubmit, msbOrTcspExists, renewalDate))
              case _ => throw new Exception("Cannot get renewal date")
            }

          }

          block getOrElse InternalServerError("Cannot get business matching or renewal date")
        }
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>

      Future.successful(Redirect(controllers.declaration.routes.WhoIsRegisteringController.get()))
  }

}
