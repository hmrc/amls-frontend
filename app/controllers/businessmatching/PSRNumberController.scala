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

package controllers.businessmatching

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import javax.inject.Inject
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberNo, BusinessAppliedForPSRNumberYes, BusinessMatching}
import models.flowmanagement.{ChangeSubSectorFlowModel, PsrNumberPageId}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.psr_number
import views.html.renewal.involved_in_other

import scala.concurrent.Future

class PSRNumberController @Inject()(val authConnector: AuthConnector,
                                    val dataCacheConnector: DataCacheConnector,
                                    val statusService: StatusService,
                                    val businessMatchingService: BusinessMatchingService,
                                    val router: Router[ChangeSubSectorFlowModel],
                                    val helper: ChangeSubSectorHelper
                                   ) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        (for {
          bm <- businessMatchingService.getModel
          status <- OptionT.liftF(statusService.getStatus)
        } yield {
          val form: Form2[BusinessAppliedForPSRNumber] = bm.businessAppliedForPSRNumber map
                  Form2[BusinessAppliedForPSRNumber] getOrElse EmptyForm
          Ok(psr_number(form, edit, bm.preAppComplete, statusService.isPreSubmission(status)))
        }) getOrElse Redirect(controllers.routes.RegistrationProgressController.get())
   }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        val route = router.getRoute(PsrNumberPageId, _: ChangeSubSectorFlowModel, edit)
        Form2[BusinessAppliedForPSRNumber](request.body) match {
          case f: InvalidForm =>
            for {
              bm <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
              status <- statusService.getStatus
            } yield bm match {
              case Some(_) => BadRequest(psr_number(f, edit, bm.preAppComplete, statusService.isPreSubmission(status)))
              case None => BadRequest(psr_number(f, edit))
            }

          case ValidForm(_, data) =>
            helper.getOrCreateFlowModel flatMap { flowModel =>
              dataCacheConnector.update[ChangeSubSectorFlowModel](ChangeSubSectorFlowModel.key) { _ =>
                flowModel.copy(psrNumber = Some(data))
              } flatMap {
                case Some(m@ChangeSubSectorFlowModel(_, Some(BusinessAppliedForPSRNumberYes(_)))) =>
                  helper.updateSubSectors(m) flatMap { _ =>
                    route(m)
                  }
                case Some(m) =>
                  route(m)
              }
            }
        }
      }
  }
}
