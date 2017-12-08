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

import config.AppConfig
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.config.inject.ServicesConfig
import org.mockito.Mockito.when

trait AppConfigSetup extends MockitoSugar {

  val appConfig = mock[AppConfig]
  val servicesConfig = mock[ServicesConfig]

  when(appConfig.config) thenReturn servicesConfig

}
