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

package controllers.businessactivities

import cats.data.OptionT
import cats.implicits._
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import services.StatusService
import utils.ControllerHelper
import views.html.businessactivities.summary

import scala.concurrent.Future


trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  implicit val statusService: StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            businessActivity <- cache.getEntry[BusinessActivities](BusinessActivities.key)
          } yield {
            ControllerHelper.allowedToEdit map(isEditable => Ok(summary(EmptyForm, businessActivity, businessMatching.activities, isEditable)))
          }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
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

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
