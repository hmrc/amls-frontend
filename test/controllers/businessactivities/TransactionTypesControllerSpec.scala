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
import models.businessactivities.{BusinessActivities, DigitalSpreadsheet, Paper, TransactionTypes, DigitalSoftware}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import org.scalatest.MustMatchers
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{never, verify}
import org.mockito.Matchers.{any, eq => eqTo}

class TransactionTypesControllerSpec extends GenericTestHelper
  with MustMatchers
  with BusinessActivitiesGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)
    val controller = new TransactionTypesController(self.authConnector, mockCacheConnector)

    mockCacheSave[BusinessActivities]
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

  "post" when {
    "called with valid data" must {
      "save the data and redirect away" in new Fixture {
        val form = Seq(
          "types[]" -> "01",
          "types[]" -> "02",
          "types[]" -> "03",
          "name" -> "example software"
        )

        val result = controller.post()(request.withFormUrlEncodedBody(form:_*))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.businessactivities.routes.IdentifySuspiciousActivityController.get().url)

        val captor = ArgumentCaptor.forClass(classOf[BusinessActivities])
        verify(mockCacheConnector).save[BusinessActivities](eqTo(BusinessActivities.key), captor.capture())(any(), any(), any())

        captor.getValue.transactionRecordTypes mustBe
          Some(TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("example software"))))

        captor.getValue.hasChanged mustBe true
        captor.getValue.hasAccepted mustBe false
      }

      "return to the summary page when in edit mode" in new Fixture {
        val form = "types[]" -> "01"

        val result = controller.post(edit = true)(request.withFormUrlEncodedBody(form))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.businessactivities.routes.SummaryController.get().url)
      }
    }

    "called with invalid data" must {
      "return BAD_REQUEST and show the page again" in new Fixture {
        val form = "types[]" -> "03"

        val result = controller.post()(request.withFormUrlEncodedBody(form))

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(messages("businessactivities.do.keep.records"))

        verify(mockCacheConnector, never).save[BusinessActivities](any(), any())(any(), any(), any())
      }
    }
  }

}
