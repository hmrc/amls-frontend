/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.deregister

import cats.data.OptionT
import cats.implicits._
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.{AmlsBaseController, CommonPlayDependencies}
import javax.inject.Inject
import models.businessmatching.BusinessMatching
import services.{AuthEnrolmentsService, StatusService}
import utils.{AuthAction, BusinessName}
import views.html.deregister.deregister_application

import scala.concurrent.Future

class DeRegisterApplicationController @Inject() (authAction: AuthAction,
                                                 val ds: CommonPlayDependencies,
                                                 implicit val cache: DataCacheConnector,
                                                 implicit val statusService: StatusService,
                                                 enrolments: AuthEnrolmentsService,
                                                 implicit val amls: AmlsConnector) extends AmlsBaseController(ds) {

  def get() = authAction.async {
        implicit request =>
          (for {
            bm <- OptionT(cache.fetch[BusinessMatching](request.credId, BusinessMatching.key))
            amlsRegNumber <- OptionT(enrolments.amlsRegistrationNumber(request.amlsRefNumber, request.groupIdentifier))
            ba <- OptionT.fromOption[Future](bm.activities)
            id <- OptionT(statusService.getSafeIdFromReadStatus(amlsRegNumber, request.accountTypeId))
            name <- BusinessName.getName(request.credId, Some(id), request.accountTypeId)
          } yield {
            val activities = ba.businessActivities map {
              _.getMessage()
            }
            Ok(deregister_application(name, activities, amlsRegNumber))
          }) getOrElse InternalServerError("Could not show the de-register page")
    }

  def post() = authAction.async {
      implicit request =>
        Future.successful(Redirect(routes.DeregistrationReasonController.get()))
  }
}
