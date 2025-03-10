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

package controllers.businessactivities

import controllers.actions.SuccessfulAuthAction
import forms.businessactivities.TransactionTypesFormProvider
import generators.businessmatching.BusinessActivitiesGenerator
import models.businessactivities.TransactionTypes.{DigitalSoftware, DigitalSpreadsheet, Paper}
import models.businessactivities.{BusinessActivities, TransactionTypes}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks}
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{never, verify}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.matchers.must.Matchers
import play.api.test.{FakeRequest, Injecting}
import views.html.businessactivities.TransactionTypesView

class TransactionTypesControllerSpec extends AmlsSpec with Matchers with BusinessActivitiesGenerator with Injecting {

  trait Fixture extends DependencyMocks { self =>
    lazy val view  = inject[TransactionTypesView]
    val request    = addToken(authRequest)
    val controller = new TransactionTypesController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      mockCacheConnector,
      mockMcc,
      inject[TransactionTypesFormProvider],
      view = view
    )

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
        html.getElementById("types_1").hasAttr("checked") mustBe true
        html.getElementById("types_2").hasAttr("checked") mustBe false
      }
    }
  }

  "post" when {
    "called with valid data" must {
      "save the data and redirect away" in new Fixture {

        val newRequest = FakeRequest(POST, routes.TransactionTypesController.post(false).url).withFormUrlEncodedBody(
          "types[1]" -> "paper",
          "types[2]" -> "digitalSpreadsheet",
          "types[3]" -> "digitalOther",
          "software" -> "example software"
        )

        val result = controller.post()(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.businessactivities.routes.IdentifySuspiciousActivityController.get().url
        )

        val captor = ArgumentCaptor.forClass(classOf[BusinessActivities])
        verify(mockCacheConnector).save[BusinessActivities](any(), eqTo(BusinessActivities.key), captor.capture())(
          any()
        )

        captor.getValue.transactionRecordTypes mustBe
          Some(TransactionTypes(Set(Paper, DigitalSpreadsheet, DigitalSoftware("example software"))))

        captor.getValue.hasChanged mustBe true
        captor.getValue.hasAccepted mustBe false
      }

      "return to the summary page when in edit mode" in new Fixture {
        val newRequest = FakeRequest(POST, routes.TransactionTypesController.post(true).url).withFormUrlEncodedBody(
          "types[1]" -> "paper"
        )

        val result = controller.post(edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.businessactivities.routes.SummaryController.get.url)
      }
    }

    "called with invalid data" must {
      "return BAD_REQUEST and show the page again" in new Fixture {
        val newRequest = FakeRequest(POST, routes.TransactionTypesController.post(true).url).withFormUrlEncodedBody(
          "types[1]" -> ""
        )

        val result = controller.post()(newRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(messages("businessactivities.do.keep.records"))

        verify(mockCacheConnector, never).save[BusinessActivities](any(), any(), any())(any())
      }
    }
  }

}
