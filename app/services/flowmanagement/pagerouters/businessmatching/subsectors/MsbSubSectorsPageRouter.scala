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

package services.flowmanagement.pagerouters.businessmatching.subsectors

import controllers.businessmatching.routes
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.flowmanagement.{ChangeSubSectorFlowModel, SubSectorsPageId}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.flowmanagement.{PageRouter, PageRouterCompanyNotRegistered}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MsbSubSectorsPageRouter extends PageRouter[ChangeSubSectorFlowModel] {

  override def getRoute(credId: String, model: ChangeSubSectorFlowModel, edit: Boolean)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] = {
    val result = model.subSectors map {
      case sectors if sectors.contains(TransmittingMoney) =>
        routes.PSRNumberController.get(edit)
      case _                                              => routes.SummaryController.get()
    }

    result.fold(error(SubSectorsPageId))(Redirect)
  }
}

class MsbSubSectorsPageRouterCompanyNotRegistered extends PageRouterCompanyNotRegistered[ChangeSubSectorFlowModel] {

  override def getRoute(
    credId: String,
    model: ChangeSubSectorFlowModel,
    edit: Boolean,
    includeCompanyNotRegistered: Boolean
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val result = model.subSectors map {
      case sectors if sectors.contains(TransmittingMoney) =>
        routes.PSRNumberController.get(edit)
      case _                                              =>
        if (includeCompanyNotRegistered) {
          routes.CheckCompanyController.get()
        } else {
          routes.SummaryController.get()
        }
    }

    result.fold(error(SubSectorsPageId))(Redirect)
  }
}
