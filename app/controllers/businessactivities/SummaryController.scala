/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessActivity.AccountancyServices
import models.businessmatching.BusinessMatching
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import utils.AuthAction
import utils.businessactivities.CheckYourAnswersHelper
import views.html.businessactivities.CheckYourAnswersView

class SummaryController @Inject() (
  val dataCache: DataCacheConnector,
  implicit val statusService: StatusService,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  cyaHelper: CheckYourAnswersHelper,
  view: CheckYourAnswersView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    dataCache.fetchAll(request.credId) map { optionalCache =>
      (for {
        cache                    <- optionalCache
        businessMatching         <- cache.getEntry[BusinessMatching](BusinessMatching.key)
        businessActivity         <- cache.getEntry[BusinessActivities](BusinessActivities.key)
        bmActivities             <- businessMatching.activities
        needsAccountancyQuestions = !bmActivities.businessActivities.contains(AccountancyServices)
      } yield {
        val summaryList = cyaHelper.createSummaryList(businessActivity, needsAccountancyQuestions)
        Ok(view(summaryList))
      }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      businessActivity <- OptionT(dataCache.fetch[BusinessActivities](request.credId, BusinessActivities.key))
      _                <-
        OptionT.liftF(
          dataCache
            .save[BusinessActivities](request.credId, BusinessActivities.key, businessActivity.copy(hasAccepted = true))
        )
    } yield Redirect(controllers.routes.RegistrationProgressController.get())) getOrElse InternalServerError(
      "Could not update HVD"
    )
  }
}
