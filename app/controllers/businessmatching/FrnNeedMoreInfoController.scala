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
import models.businessmatching.{BusinessAppliedForFrn, BusinessAppliedForFrnYes}
import models.flowmanagement.{ChangeSubSectorFlowModel, FrnNeedMoreInfoPageId}
import play.api.Logging
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

  def post(edit: Boolean = false, includeCompanyNotRegistered: Boolean = false): Action[AnyContent] = authAction.async {
    implicit request =>
      val route = router.getRoute(
        request.credId,
        FrnNeedMoreInfoPageId,
        _: ChangeSubSectorFlowModel,
        edit,
        includeCompanyNotRegistered
      )

      dataCacheConnector.update[ChangeSubSectorFlowModel](request.credId, ChangeSubSectorFlowModel.key) {
        case Some(flowModel) =>
          flowModel.copy(Frn = flowModel.Frn.map(transformFrn))
        case None =>
          throw new Exception("Flow model not found")
      } flatMap {
        case Some(updatedFlowModel @ ChangeSubSectorFlowModel(_, Some(BusinessAppliedForFrnYes(_)))) =>
          helper.updateSubSectors(request.credId, updatedFlowModel) flatMap { _ =>
            route(updatedFlowModel)
          }
        case None => throw new Exception("Flow model not updated")
      }
  }

  private def transformFrn(data: BusinessAppliedForFrn): BusinessAppliedForFrn =
    data match {
      case BusinessAppliedForFrnYes(regNumber) if regNumber.length == 7 =>
        BusinessAppliedForFrnYes("700000")
      case other =>
        other
    }
}
