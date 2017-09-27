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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching}
import models.status.{NotCompleted, SubmissionReady, SubmissionStatus}
import play.api.Play
import services.StatusService
import services.businessmatching.BusinessMatchingService
import views.html.businessmatching.summary
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector
  protected def businessMatchingService: BusinessMatchingService
  protected def statusService: StatusService

  def get() = Authorised.async {
    implicit authContext => implicit request =>
        def isPreSubmission(status: SubmissionStatus) = Set(NotCompleted, SubmissionReady).contains(status)

        val okResult = for {
          bm <- businessMatchingService.getModel
          ba <- OptionT.fromOption[Future](bm.activities)
          status <- OptionT.liftF(statusService.getStatus)
        } yield {
          val bmWithAdditionalActivities = bm.copy(
            activities = Some(BusinessActivities(
              ba.businessActivities ++ ba.additionalActivities.fold[Set[BusinessActivity]](Set.empty)(act => act)
            ))
          )
          Ok(summary(EmptyForm, bmWithAdditionalActivities, isPreSubmission(status) || ApplicationConfig.businessMatchingVariationToggle))
        }

        okResult getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      (for {
        businessMatching <- businessMatchingService.getModel
        businessActivities <- OptionT.fromOption[Future](businessMatching.activities)
        _ <- businessMatchingService.updateModel(businessMatching.copy(hasAccepted = true))
        _ <- businessMatchingService.commitVariationData map { _ => true } orElse OptionT.some(false)
      } yield {
        if(businessActivities.additionalActivities.isDefined){
          Redirect(controllers.businessmatching.updateservice.routes.TradingPremisesController.get())
        } else {
          Redirect(controllers.routes.RegistrationProgressController.get())
        }
      }) getOrElse InternalServerError("Unable to update business matching")
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
  override val businessMatchingService = Play.current.injector.instanceOf[BusinessMatchingService]
}
