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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import javax.inject.{Inject, Singleton}
import models.businessmatching.{BusinessActivities, BusinessActivity}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.summary

import scala.concurrent.Future

@Singleton
class SummaryController @Inject()(
                                   val dataCache: DataCacheConnector,
                                   val authConnector: AuthConnector,
                                   val statusService: StatusService,
                                   val businessMatchingService: BusinessMatchingService
                                 ) extends BaseController {

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>

        (for {
          bm <- businessMatchingService.getModel
          ba <- OptionT.fromOption[Future](bm.activities)
          status <- OptionT.liftF(statusService.getStatus)
        } yield {
          val isPreSubmission = statusService.isPreSubmission(status)
          val bmWithAdditionalActivities = bm.copy(
            activities = Some(BusinessActivities(
              ba.businessActivities ++ ba.additionalActivities.fold[Set[BusinessActivity]](Set.empty)(act => act)
            ))
          )

          val changeActivitiesUrl = if (isPreSubmission) {
            controllers.businessmatching.routes.RegisterServicesController.get().url
          } else {
            controllers.businessmatching.updateservice.routes.ChangeBusinessTypesController.get().url
          }

          Ok(summary(EmptyForm,
            bmWithAdditionalActivities,
            changeActivitiesUrl,
            isPreSubmission,
            statusService.isPending(status)))

        }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
  }

  def post() = Authorised.async {
    implicit authContext =>
      implicit request => {
        for {
          businessMatching <- businessMatchingService.getModel
          _ <- businessMatchingService.updateModel(businessMatching.copy(hasAccepted = true, preAppComplete = true))
        } yield Redirect(controllers.routes.RegistrationProgressController.get())
      } getOrElse InternalServerError("Unable to update business matching")
  }

}
