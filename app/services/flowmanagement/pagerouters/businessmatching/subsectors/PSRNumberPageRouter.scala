/*
 * Copyright 2019 HM Revenue & Customs
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

package services.flowmanagement.pagerouters.businessmatching.subsectors

import controllers.businessmatching.routes
import models.businessmatching.BusinessAppliedForPSRNumberYes
import models.flowmanagement.{PsrNumberPageId, ChangeSubSectorFlowModel}
import play.api.mvc.Result
import services.flowmanagement.PageRouter
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.mvc.Results.Redirect
import scala.concurrent.{ExecutionContext, Future}

class PSRNumberPageRouter extends PageRouter[ChangeSubSectorFlowModel] {
  override def getPageRoute(model: ChangeSubSectorFlowModel, edit: Boolean)
                           (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val call = model.psrNumber map {
      case BusinessAppliedForPSRNumberYes(_) => routes.SummaryController.get()
      case _ => routes.NoPsrController.get()
    }

    call.fold(error(PsrNumberPageId))(Redirect)
  }
}
