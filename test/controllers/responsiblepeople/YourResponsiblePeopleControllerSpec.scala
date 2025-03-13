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

package controllers.responsiblepeople

import controllers.actions.SuccessfulAuthAction
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.Injecting
import services.responsiblepeople.YourResponsiblePeopleService
import utils.AmlsSpec
import views.html.responsiblepeople.YourResponsiblePeopleView

import scala.concurrent.Future

class YourResponsiblePeopleControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    val request     = addToken(authRequest)
    val mockService = mock[YourResponsiblePeopleService]
    lazy val view   = app.injector.instanceOf[YourResponsiblePeopleView]
    val controller  = new YourResponsiblePeopleController(
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      yourResponsiblePeopleService = mockService,
      view = view
    )
  }

  "Get" must {

    "load the your answers page when section data is available" in new Fixture {
      val model  = ResponsiblePerson(None, None)
      when(mockService.completeAndIncompleteRP(any()))
        .thenReturn(Future.successful(Some((Seq.empty, Seq(model).zipWithIndex.reverse))))
      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(
        s"${messages("responsiblepeople.whomustregister.title")} - ${messages("summary.responsiblepeople")}"
      )
    }

    "show the 'Add a responsible person' link" in new Fixture {

      val rp1 = ResponsiblePerson(Some(PersonName("firstName1", Some("middleName"), "lastName1")))
      val rp2 = ResponsiblePerson(Some(PersonName("firstName2", None, "lastName2")))

      when(mockService.completeAndIncompleteRP(any()))
        .thenReturn(Future.successful(Some((Seq(rp1).zipWithIndex, Seq(rp2).zipWithIndex))))

      val result = controller.get()(request)
      status(result) must be(OK)

      contentAsString(result) must include(messages("responsiblepeople.check_your_answers.add"))

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("addResponsiblePerson").attr("href") must be(
        routes.ResponsiblePeopleAddController.get(false).url
      )

    }

    "correctly display responsible people's full names" in new Fixture {

      val rp1 = ResponsiblePerson(Some(PersonName("firstName1", Some("middleName"), "lastName1")))
      val rp2 = ResponsiblePerson(Some(PersonName("firstName2", None, "lastName2")))

      when(mockService.completeAndIncompleteRP(any()))
        .thenReturn(Future.successful(Some((Seq(rp1).zipWithIndex, Seq(rp2).zipWithIndex))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = contentAsString(result)
      document must include("firstName1 middleName lastName1")
      document must include("firstName2 lastName2")

    }

    "redirect to the main AMLS summary page when section data is unavailable" in new Fixture {
      when(mockService.completeAndIncompleteRP(any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
    }
  }
}
