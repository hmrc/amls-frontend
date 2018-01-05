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

package controllers.supervision

import models.supervision.{AssociationOfBookkeepers, BusinessTypes, Other, Supervision}
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class WhichProfessionalBodyControllerSpec extends PlaySpec with GenericTestHelper with MockitoSugar{

  trait Fixture extends DependencyMocks with AuthorisedFixture { self =>

    val request = addToken(authRequest)

    val controller = new WhichProfessionalBodyController(
      mockCacheConnector,
      self.authConnector
    )

  }

  "WhichProfessionalBodyControllerSpec" when {

    "get" must {

      "display view" when {

        "form data exists" in new Fixture {

          mockCacheFetch[Supervision](Some(Supervision(
            businessTypes = Some(BusinessTypes(Set(AssociationOfBookkeepers, Other("SomethingElse"))))
          )))

          val result = controller.get()(request)

          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          document.title() must include(Messages("supervision.whichprofessionalbody.title"))
          document.select("input[value=12]").hasAttr("checked") must be(true)
          document.select("input[value=14]").hasAttr("checked") must be(true)
          document.select("input[name=specifyOtherBusiness]").`val`() must be("SomethingElse")

        }

        "form data is empty" in new Fixture {

          mockCacheFetch[Supervision](None)

          val result = controller.get()(request)

          status(result) must be(OK)

          Jsoup.parse(contentAsString(result)).title() must include(Messages("supervision.whichprofessionalbody.title"))

          val document = Jsoup.parse(contentAsString(result))

          document.title() must include(Messages("supervision.whichprofessionalbody.title"))

          document.select("input[type=checkbox]").hasAttr("checked") must be(false)
          document.select("input[name=specifyOtherBusiness]").`val`() must be(empty)
        }
      }
    }

    "post" when {

      "be called" in new Fixture {

        val result = controller.post()(request.withFormUrlEncodedBody())

      }

    }

  }

}
