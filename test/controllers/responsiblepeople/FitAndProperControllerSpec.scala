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

package controllers.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

class FitAndProperControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .configure("microservice.services.feature-toggle.show-fees" -> true)
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[FitAndProperController]

  }

  val testFitAndProper = Some(true)

  "BusinessRegisteredForVATController" when {

    "get is called" must {
      "respond with OK" when {
        "there is a PersonName and value for hasAlreadyPassedFitAndProper present" in new Fixture {

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            personName = Some(PersonName("firstName", None, "lastName")), hasAlreadyPassedFitAndProper = testFitAndProper
          ))), Some(ResponsiblePerson.key))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=true]").hasAttr("checked") must be(true)
          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=false]").hasAttr("checked") must be(false)

        }

        "there is a PersonName but has not passed fit and proper" in new Fixture {

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            personName = Some(PersonName("firstName", None, "lastName")), hasAlreadyPassedFitAndProper = Some(false)
          ))), Some(ResponsiblePerson.key))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=true]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=false]").hasAttr("checked") must be(true)

        }

        "there is a PersonName but no value for hasAlreadyPassedFitAndProper" in new Fixture {

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            personName = Some(PersonName("firstName", None, "lastName")), hasAlreadyPassedFitAndProper = None
          ))), Some(ResponsiblePerson.key))

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=true]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=false]").hasAttr("checked") must be(false)

        }
      }

      "respond with NOT_FOUND" when {
        "there is no PersonName present" in new Fixture {

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            personName = None, hasAlreadyPassedFitAndProper = None
          ))), Some(ResponsiblePerson.key))

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" must {
      "respond with NOT_FOUND" when {
        "the index is out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            hasAlreadyPassedFitAndProper = testFitAndProper
          ))), Some(ResponsiblePerson.key))

          mockCacheSave[Seq[ResponsiblePerson]]

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "invalid"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            hasAlreadyPassedFitAndProper = testFitAndProper
          ))), Some(ResponsiblePerson.key))

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with SEE_OTHER" when {
        "given valid data and edit = false, and redirect to the PersonRegisteredController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            hasAlreadyPassedFitAndProper = testFitAndProper
          ))), Some(ResponsiblePerson.key))

          mockCacheSave[Seq[ResponsiblePerson]]

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.PersonRegisteredController.get(1).url))
        }

        "given valid data and edit = true, and redirect to the DetailedAnswersController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          mockCacheFetch[Seq[ResponsiblePerson]](Some(Seq(ResponsiblePerson(
            hasAlreadyPassedFitAndProper = testFitAndProper
          ))), Some(ResponsiblePerson.key))

          mockCacheSave[Seq[ResponsiblePerson]]

          val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, true, Some(flowFromDeclaration)).url))
        }
      }
    }
  }
}


