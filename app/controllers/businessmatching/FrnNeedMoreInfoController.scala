/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import controllers.{AmlsBaseController, CommonPlayDependencies}
import models.businessmatching.BusinessAppliedForFrn
import models.flowmanagement.{ChangeSubSectorFlowModel, FrnNeedMoreInfoPageId, FrnPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.flowmanagement.Router2
import utils.AuthAction
import views.html.businessmatching.FrnNeedMoreInfoView
import javax.inject.{Inject, Singleton}

@Singleton
class FrnNeedMoreInfoController @Inject() (
  authAction: AuthAction,
  val ds: CommonPlayDependencies,
  val cc: MessagesControllerComponents,
  val router: Router2[ChangeSubSectorFlowModel],
  val dataCacheConnector: DataCacheConnector,
  val helper: ChangeSubSectorHelper,
  view: FrnNeedMoreInfoView
) extends AmlsBaseController(ds, cc) {

  def get(): Action[AnyContent] = authAction { implicit request =>
    Ok(view())
  }

  def post(edit: Boolean = false, includeCompanyNotRegistered: Boolean = true): Action[AnyContent] = authAction.async {
    implicit request =>
      val route = router.getRoute(
        request.credId,
        FrnNeedMoreInfoPageId,
        _: ChangeSubSectorFlowModel,
        edit,
        includeCompanyNotRegistered
      )

      dataCacheConnector.fetch[ChangeSubSectorFlowModel](request.credId, ChangeSubSectorFlowModel.key) flatMap {
        case Some(flowModel) => route(flowModel)
        case None            => throw new Exception("Flow model not found")
      }
  }
}
