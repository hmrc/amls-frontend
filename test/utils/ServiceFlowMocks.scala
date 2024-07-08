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

package utils

import models.businessmatching.BusinessActivity
import org.scalatestplus.mockito.MockitoSugar
import services.businessmatching.ServiceFlow
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future

trait ServiceFlowMocks extends MockitoSugar {

  implicit val mockServiceFlow: ServiceFlow = mock[ServiceFlow]

  def mockIsNewActivityNewAuth(value: Boolean, activity: Option[BusinessActivity] = None) =
    activity map { a =>
      when(mockServiceFlow.isNewActivity(any(), eqTo(a))(any())) thenReturn Future.successful(value)
    } getOrElse when(mockServiceFlow.isNewActivity(any(), any())(any())) thenReturn Future.successful(value)
}
