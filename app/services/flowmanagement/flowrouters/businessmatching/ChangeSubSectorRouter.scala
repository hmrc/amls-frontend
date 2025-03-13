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

package services.flowmanagement.flowrouters.businessmatching

import javax.inject.Inject
import models.flowmanagement._
import play.api.mvc.Result
import services.flowmanagement.{Router, Router2}
import services.flowmanagement.pagerouters.businessmatching.subsectors._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
// Individual page routers are tested, plus acceptance tests are
// testing the flow
class ChangeSubSectorRouter @Inject() (
  subSectorRouter: MsbSubSectorsPageRouter,
  psrNumberRouter: PSRNumberPageRouter,
  noPsrRouter: NoPsrNumberPageRouter
) extends Router[ChangeSubSectorFlowModel] {

  override def getRoute(credId: String, pageId: PageId, model: ChangeSubSectorFlowModel, edit: Boolean)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] = pageId match {
    case SubSectorsPageId => subSectorRouter.getRoute(credId, model, edit)
    case PsrNumberPageId  => psrNumberRouter.getRoute(credId, model, edit)
    case NoPSRPageId      => noPsrRouter.getRoute(credId, model, edit)
    case _                => throw new Exception("An Unknown Exception has occurred : ChangeSubSectorRouter")
  }
}

class ChangeSubSectorRouter2 @Inject() (
  subSectorRouter: MsbSubSectorsPageRouterCompanyNotRegistered,
  psrNumberRouter: PSRNumberPageRouterCompanyNotRegistered,
  noPsrRouter: NoPsrNumberPageRouterCompanyNotRegistered
) extends Router2[ChangeSubSectorFlowModel] {

  override def getRoute(
    credId: String,
    pageId: PageId,
    model: ChangeSubSectorFlowModel,
    edit: Boolean,
    includeCompanyNotRegistered: Boolean
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = pageId match {
    case SubSectorsPageId => subSectorRouter.getRoute(credId, model, edit, includeCompanyNotRegistered)
    case PsrNumberPageId  => psrNumberRouter.getRoute(credId, model, edit, includeCompanyNotRegistered)
    case NoPSRPageId      => noPsrRouter.getRoute(credId, model, edit, includeCompanyNotRegistered)
    case _                => throw new Exception("An Unknown Exception has occurred : ChangeSubSectorRouter")
  }
}

// $COVERAGE-ON$
