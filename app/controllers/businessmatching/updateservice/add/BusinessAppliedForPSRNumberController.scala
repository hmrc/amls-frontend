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

package controllers.businessmatching.updateservice.add

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.businessmatching.updateservice.UpdateServiceHelper
import javax.inject.{Inject, Singleton}
import models.businessmatching._
import models.flowmanagement.{AddServiceFlowModel, BusinessAppliedForPSRNumberPageId}
import services.StatusService
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching.business_applied_for_psr_number

import scala.concurrent.Future


@Singleton
class BusinessAppliedForPSRNumberController @Inject()(
                                   val authConnector: AuthConnector,
                                   implicit val dataCacheConnector: DataCacheConnector,
                                   val statusService: StatusService,
                                   val businessMatchingService: BusinessMatchingService,
                                   val helper: UpdateServiceHelper,
                                   val router: Router[AddServiceFlowModel]
                                 ) extends BaseController {


  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        businessMatchingService.getModel.value map { maybeBm =>
          val form: Form2[BusinessAppliedForPSRNumber] = (for {
            bm <- maybeBm
            number <- bm.businessAppliedForPSRNumber
          } yield Form2[BusinessAppliedForPSRNumber](number)).getOrElse(EmptyForm)
          Ok(business_applied_for_psr_number(form, edit, maybeBm.fold(false)(_.preAppComplete)))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[BusinessAppliedForPSRNumber](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(business_applied_for_psr_number(f, edit)))

          case ValidForm(_, data) => {
            dataCacheConnector.update[AddServiceFlowModel](AddServiceFlowModel.key) {
              case Some(model) => {
                model
              }
            } flatMap {
              case Some(model) => {
                router.getRoute(BusinessAppliedForPSRNumberPageId, model, edit)
              }
              case _ => Future.successful(InternalServerError("Cannot retrieve data"))
            }
          }
        }
//      {
//        Form2[BusinessAppliedForPSRNumber](request.body) match {
//          case f: InvalidForm =>
//            Future.successful(BadRequest(business_applied_for_psr_number(f, edit)))
//          case ValidForm(_, BusinessAppliedForPSRNumberYes(x)) => {
//            (for {
//              bm <- businessMatchingService.getModel
//              _ <- businessMatchingService.updateModel(
//                bm.businessAppliedForPSRNumber(BusinessAppliedForPSRNumberYes(x))
//              )
//            } yield {
//              Redirect(routes.SummaryController.get())
//            }) getOrElse InternalServerError("Could not update psr number")
//          }
//          case ValidForm(_, _) =>
//            Future.successful(Redirect(routes.NoPsrController.get()))
//        }
//      }
  }
}
