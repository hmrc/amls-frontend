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

package modules

import com.google.inject.{AbstractModule, TypeLiteral}
import models.businessmatching.updateservice.ChangeBusinessType
import models.flowmanagement.{AddBusinessTypeFlowModel, ChangeSubSectorFlowModel, RemoveBusinessTypeFlowModel}
import services.flowmanagement.{Router, Router2}
import services.flowmanagement.flowrouters.businessmatching.{AddBusinessTypeRouter, ChangeBusinessTypeRouter, ChangeSubSectorRouter, ChangeSubSectorRouter2, RemoveBusinessTypeRouter}

import java.time.Clock

class Module extends AbstractModule {

  override def configure() = {
    bind(new TypeLiteral[Router[AddBusinessTypeFlowModel]] {}).to(classOf[AddBusinessTypeRouter])
    bind(new TypeLiteral[Router[ChangeBusinessType]] {}).to(classOf[ChangeBusinessTypeRouter])
    bind(new TypeLiteral[Router[RemoveBusinessTypeFlowModel]] {}).to(classOf[RemoveBusinessTypeRouter])
    bind(new TypeLiteral[Router[ChangeSubSectorFlowModel]] {}).to(classOf[ChangeSubSectorRouter])
    bind(new TypeLiteral[Router2[ChangeSubSectorFlowModel]] {}).to(classOf[ChangeSubSectorRouter2])
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone())
  }
}
