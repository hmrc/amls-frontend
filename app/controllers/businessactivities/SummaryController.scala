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

package controllers.businessactivities

import cats.data.OptionT
import cats.implicits._
import com.google.inject.Inject
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.businessactivities.BusinessActivities
import models.businessmatching.{AccountancyServices, BusinessMatching}
import services.StatusService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.ControllerHelper
import views.html.businessactivities.summary

import scala.concurrent.Future

class SummaryController @Inject() (val dataCache: DataCacheConnector,
                                   implicit val statusService: StatusService,
                                   override val authConnector: AuthConnector
                                  )extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            businessActivity <- cache.getEntry[BusinessActivities](BusinessActivities.key)
            bmActivities <- businessMatching.activities
          } yield {
            val hideReceiveAdvice = bmActivities.businessActivities.contains(AccountancyServices)
              Ok(summary(EmptyForm, businessActivity, businessMatching.alphabeticalBusinessTypes, hideReceiveAdvice))
          }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }

  def post = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        businessActivity <- OptionT(dataCache.fetch[BusinessActivities](BusinessActivities.key))
        _ <- OptionT.liftF(dataCache.save[BusinessActivities](BusinessActivities.key,
          businessActivity.copy(hasAccepted = true))
        )
      } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError("Could not update HVD")
  }
}
