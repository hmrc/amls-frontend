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

package controllers.deregister

import javax.inject.Inject

import cats.data.OptionT
import connectors.{AmlsConnector, DataCacheConnector}
import controllers.BaseController
import models.businessmatching.BusinessMatching
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.deregister.deregister_application

import scala.concurrent.Future
import cats.implicits._
import config.ApplicationConfig

class DeRegisterApplicationController @Inject()
(
  val authConnector: AuthConnector,
  cache: DataCacheConnector,
  statusService: StatusService,
  enrolments: AuthEnrolmentsService,
  amls: AmlsConnector
) extends BaseController {

  def get() = {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          (for {
            bm <- OptionT(cache.fetch[BusinessMatching](BusinessMatching.key))
            details <- OptionT.fromOption[Future](bm.reviewDetails)
            amlsRegNumber <- OptionT(enrolments.amlsRegistrationNumber)
            ba <- OptionT.fromOption[Future](bm.activities)
          } yield {
            val activities = ba.businessActivities map {
              _.getMessage()
            }

            Ok(deregister_application(details.businessName, activities, amlsRegNumber))
          }) getOrElse InternalServerError("Could not show the de-register page")
    }
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request =>
        Future.successful(Redirect(routes.DeregistrationReasonController.get()))
  }
}
