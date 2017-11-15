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

package controllers.businessactivities

import generators.businessmatching.BusinessActivitiesGenerator
import models.businessactivities.{BusinessActivities, Paper, TransactionTypes}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import org.scalatest.MustMatchers
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class TransactionTypesControllerSpec extends GenericTestHelper
  with MustMatchers
  with BusinessActivitiesGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)
    val controller = new TransactionTypesController(self.authConnector, mockCacheConnector)

    mockCacheFetch(Some(BusinessActivities()))
  }

  "get" when {
    "called" must {
      "return OK status with a blank form" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include(messages("businessactivities.do.keep.records"))
      }

      "return OK status with a populated form" in new Fixture {
        val model = BusinessActivities(transactionRecordTypes = Some(TransactionTypes(Set(Paper))))
        mockCacheFetch(Some(model))

        val result = controller.get()(request)
        status(result) mustBe OK

        val html = Jsoup.parse(contentAsString(result))
        html.select("input[type=checkbox][value=\"01\"]").first().attr("checked") mustBe "checked"
        html.select("input[type=checkbox][value=\"02\"]").first().attr("checked") must not be "checked"
      }
    }
  }

}
