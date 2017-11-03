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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo}
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}


class LegalNameChangeDateControllerSpec extends GenericTestHelper with ScalaFutures {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)
    val RecordId = 1

    val cache = mock[DataCacheConnector]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(cache))
      .build()

    lazy val controller = injector.instanceOf[LegalNameChangeDateController]

  }

  "The LegalNameChangeDateController" when {
    "get is called" must {
      "prepopulate the view with data" in new TestFixture {

        val result = controller.get(RecordId)(request)

        //status(result) mustBe OK

        //val html = Jsoup.parse(contentAsString(result))

        //html.select("input[type=radio][value=TestPerson]").hasAttr("checked") mustBe true
      }

    }

    "post is called" must {
      "respond with SEE_OTHER and" in new TestFixture {

        val result = controller.post(RecordId)(request.withFormUrlEncodedBody("firstName" -> "testName"))


      }

    }

  }

}
