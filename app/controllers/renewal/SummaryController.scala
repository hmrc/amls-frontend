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

package controllers.renewal

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessMatching
import models.renewal.Renewal
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ProgressService, RenewalService}
import utils.AuthAction
import utils.renewal.CheckYourAnswersHelper
import views.html.renewal.CheckYourAnswersView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SummaryController @Inject() (
  val dataCacheConnector: DataCacheConnector,
  val authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val renewalService: RenewalService,
  val progressService: ProgressService,
  val cc: MessagesControllerComponents,
  cyaHelper: CheckYourAnswersHelper,
  view: CheckYourAnswersView
) extends AmlsBaseController(ds, cc) {

  def get: Action[AnyContent] = authAction.async { implicit request =>
    dataCacheConnector.fetchAll(request.credId) flatMap { optionalCache =>
      (for {
        cache            <- OptionT.fromOption[Future](optionalCache)
        businessMatching <- OptionT.fromOption[Future](cache.getEntry[BusinessMatching](BusinessMatching.key))
        renewal          <- OptionT.fromOption[Future](cache.getEntry[Renewal](Renewal.key))
      } yield {
        val summaryList = cyaHelper.getSummaryList(renewal, businessMatching)
        Ok(view(summaryList))
      }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
    }
  }

  def post: Action[AnyContent] = authAction.async { implicit request =>
    (for {
      renewal <- OptionT(dataCacheConnector.fetch[Renewal](request.credId, Renewal.key))
      _       <-
        OptionT.liftF(dataCacheConnector.save[Renewal](request.credId, Renewal.key, renewal.copy(hasAccepted = true)))
    } yield Redirect(controllers.renewal.routes.RenewalProgressController.get)) getOrElse InternalServerError(
      "Could not update renewal"
    )
  }
}
