/*
 * Copyright 2017 HM Revenue & Customs
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

import models.status.SubmissionStatus
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import services.StatusService

import scala.concurrent.Future

trait StatusMocks extends MockitoSugar {

  implicit val mockStatusService = mock[StatusService]

  def mockApplicationStatus(status: SubmissionStatus)(implicit service: StatusService) = when {
    service.getStatus(any(), any(), any())
  } thenReturn Future.successful(status)

}
