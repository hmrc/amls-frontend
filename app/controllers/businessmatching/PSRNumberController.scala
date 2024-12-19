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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import controllers.{AmlsBaseController, CommonPlayDependencies}
import forms.businessmatching.PSRNumberFormProvider
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberYes}
import models.flowmanagement.{ChangeSubSectorFlowModel, PsrNumberPageId}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router2
import utils.AuthAction
import views.html.businessmatching.PsrNumberView

import javax.inject.Inject

class PSRNumberController @Inject()(authAction: AuthAction,
                                    val ds: CommonPlayDependencies,
                                    val dataCacheConnector: DataCacheConnector,
                                    val statusService: StatusService,
                                    val businessMatchingService: BusinessMatchingService,
                                    val router: Router2[ChangeSubSectorFlowModel],
                                    val helper: ChangeSubSectorHelper,
                                    val cc: MessagesControllerComponents,
                                    formProvider: PSRNumberFormProvider,
                                    psr_number: PsrNumberView) extends AmlsBaseController(ds, cc) {

  def get(edit: Boolean = false): Action[AnyContent] = authAction.async {
      implicit request =>
        (for {
          bm <- businessMatchingService.getModel(request.credId)
          status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
        } yield {
          val form = bm.businessAppliedForPSRNumber.fold(formProvider())(formProvider().fill)

          Ok(psr_number(form, edit, bm.preAppComplete, statusService.isPreSubmission(status), bm.businessAppliedForPSRNumber.isDefined))
        }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
   }

  def post(edit: Boolean = false, includeCompanyNotRegistered: Boolean = false): Action[AnyContent] = authAction.async {
      implicit request => {
        val route = router.getRoute(request.credId, PsrNumberPageId, _: ChangeSubSectorFlowModel, edit, includeCompanyNotRegistered)

        formProvider().bindFromRequest().fold(
          formWithErrors => {
            (for {
              bm <- businessMatchingService.getModel(request.credId)
              status <- OptionT.liftF(statusService.getStatus(request.amlsRefNumber, request.accountTypeId, request.credId))
            } yield {
              BadRequest(psr_number(formWithErrors, edit, bm.preAppComplete, statusService.isPreSubmission(status)))
            }) getOrElse BadRequest(psr_number(formWithErrors, edit))
          },
          data => {
            helper.getOrCreateFlowModel(request.credId) flatMap { flowModel =>
              dataCacheConnector.update[ChangeSubSectorFlowModel](request.credId, ChangeSubSectorFlowModel.key) { _ =>
                flowModel.copy(psrNumber = Some(transformPSRNumber(data)))
              } flatMap {
                case Some(m @ ChangeSubSectorFlowModel(_, Some(BusinessAppliedForPSRNumberYes(_)))) =>
                  helper.updateSubSectors(request.credId, m) flatMap { _ =>
                    route(m)
                  }
                case Some(m) =>
                  route(m)
                case _ => throw new Exception("An Exception has occurred")
              }
            }
          }
        )
      }
  }

  private def transformPSRNumber(data: BusinessAppliedForPSRNumber): BusinessAppliedForPSRNumber = {
    data match {
      case BusinessAppliedForPSRNumberYes(regNumber) if regNumber.length == 7 =>
        BusinessAppliedForPSRNumberYes("700000")
      case other =>
        other
    }
  }
}
