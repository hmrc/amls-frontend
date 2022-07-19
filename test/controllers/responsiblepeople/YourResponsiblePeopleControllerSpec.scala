/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.actions.SuccessfulAuthAction
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import utils.AmlsSpec
import play.api.i18n.Messages
import play.api.test.Helpers._
import views.html.responsiblepeople.your_responsible_people

import scala.concurrent.Future

class YourResponsiblePeopleControllerSpec extends AmlsSpec with MockitoSugar {

    trait Fixture {
      self => val request = addToken(authRequest)
      lazy val view = app.injector.instanceOf[your_responsible_people]
      val controller = new YourResponsiblePeopleController (
        dataCacheConnector = mock[DataCacheConnector],
        authAction = SuccessfulAuthAction, ds = commonDependencies, cc = mockMcc,
        your_responsible_people = view)
    }

    "Get" must {

      "load the your answers page when section data is available" in new Fixture {
        val model = ResponsiblePerson(None, None)
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(model))))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (s"${Messages("responsiblepeople.whomustregister.title")} - ${Messages("summary.responsiblepeople")}")
      }

      "show the 'Add a responsible person' link" in new Fixture {

        val rp1 = ResponsiblePerson(Some(PersonName("firstName1", Some("middleName"), "lastName1")))
        val rp2 = ResponsiblePerson(Some(PersonName("firstName2", None, "lastName2")))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(rp2, rp1))))

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include (Messages("responsiblepeople.check_your_answers.add"))

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("addResponsiblePerson").attr("href") must be (routes.ResponsiblePeopleAddController.get(false).url)

      }

      "correctly display responsible people's full names" in new Fixture {

        val rp1 = ResponsiblePerson(Some(PersonName("firstName1", Some("middleName"), "lastName1")))
        val rp2 = ResponsiblePerson(Some(PersonName("firstName2", None, "lastName2")))

        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(rp2, rp1))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = contentAsString(result)
        document must include ("firstName1 middleName lastName1")
        document must include ("firstName2 lastName2")

      }

      "redirect to the main AMLS summary page when section data is unavailable" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      }
    }
  }
