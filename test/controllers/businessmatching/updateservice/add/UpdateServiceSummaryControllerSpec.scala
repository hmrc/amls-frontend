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

package controllers.businessmatching.updateservice.add

import org.junit.Assert
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}


class UpdateServicesSummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new UpdateServicesSummaryController(
      dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector
    )
  }

  "Get" must {

    "load the summary page with the correct link text when the section is incomplete" in new Fixture {
      fail()
    }


    "load the summary page with the correct link text when the section is complete" in new Fixture {
      fail()
    }

  }

  "post is called" must {
    "respond with OK and redirect to the 'do you want to add more activities' page if the user clicks continue" when {

      "all questions are complete" in new Fixture {
        fail()
      }
    }
  }
}

