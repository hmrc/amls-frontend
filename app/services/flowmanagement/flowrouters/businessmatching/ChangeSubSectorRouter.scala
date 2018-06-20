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

package services.flowmanagement.flowrouters.businessmatching

import javax.inject.Inject
import models.flowmanagement._
import play.api.mvc.Result
import services.flowmanagement.Router
import services.flowmanagement.pagerouters.businessmatching.subsectors.{MsbSubSectorsPageRouter, NoPsrNumberPageRouter, PSRNumberPageRouter}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE_OFF$
// Individual page routers are tested, plus acceptance tests are
// testing the flow
class ChangeSubSectorRouter @Inject()(
                                     subSectorRouter: MsbSubSectorsPageRouter,
                                     psrNumberRouter: PSRNumberPageRouter,
                                     noPsrRouter: NoPsrNumberPageRouter
                                     ) extends Router[ChangeSubSectorFlowModel] {
  override def getRoute(pageId: PageId, model: ChangeSubSectorFlowModel, edit: Boolean)
                       (implicit ac: AuthContext, hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = pageId match {
    case SubSectorsPageId => subSectorRouter.getPageRoute(model, edit)
    case PsrNumberPageId => psrNumberRouter.getPageRoute(model, edit)
    case NoPSRPageId => noPsrRouter.getPageRoute(model, edit)
  }
}
// $COVERAGE_ON$