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

package services

import connectors.EnrolmentStoreConnector
import generators.AmlsReferenceNumberGenerator
import org.mockito.Matchers.{eq => eqTo}
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

class EnrolmentStoreServiceSpec extends PlaySpec with MustMatchers with MockitoSugar with AmlsReferenceNumberGenerator {

  trait Fixture {
    implicit val hc = HeaderCarrier()
    implicit val authContext = mock[AuthContext]

    val connector = mock[EnrolmentStoreConnector]
    val service = new EnrolmentStoreService(connector)

  }

}
