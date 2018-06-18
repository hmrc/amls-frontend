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

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import javax.inject.Inject
import models.businessmatching._
import models.flowmanagement.{ChangeSubSectorFlowModel, SubSectorsPageId}
import services.businessmatching.BusinessMatchingService
import services.flowmanagement.Router
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class MsbSubSectorsController @Inject()(val authConnector: AuthConnector,
                                        val dataCacheConnector: DataCacheConnector,
                                        val router: Router[ChangeSubSectorFlowModel],
                                        val businessMatchingService: BusinessMatchingService) extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        businessMatchingService.getModel.value map { maybeBM =>
          val form = (for {
            bm <- maybeBM
            services <- bm.msbServices
          } yield Form2[BusinessMatchingMsbServices](services)).getOrElse(EmptyForm)

          Ok(views.html.businessmatching.services(form, edit, maybeBM.fold(false)(_.preAppComplete)))
        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext =>
      implicit request =>
        Form2[BusinessMatchingMsbServices](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.businessmatching.services(f, edit)))

          case ValidForm(_, data) =>

            dataCacheConnector.update[ChangeSubSectorFlowModel](ChangeSubSectorFlowModel.key) { maybeModel =>
              maybeModel.getOrElse(ChangeSubSectorFlowModel()).copy(subSectors = Some(data.msbServices))
            } flatMap {
              case Some(updatedModel) => router.getRoute(SubSectorsPageId, updatedModel)
            }
        }
  }
}
