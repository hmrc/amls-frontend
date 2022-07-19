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
import models.Country
import models.autocomplete.{CountryDataProvider, NameValuePair}
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthAction}

import scala.concurrent.Future

class CountryOfBirthControllerSpec extends AmlsSpec with MockitoSugar with NinoUtil {

  val RecordId = 1

  trait Fixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[CountryDataProvider].to(new CountryDataProvider {
        override def fetch: Option[Seq[NameValuePair]] = Some(Seq(
          NameValuePair("Spain", "ES")
        ))
      }))
      .build()

    val controllers = app.injector.instanceOf[CountryOfBirthController]
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99
  val personName = Some(PersonName("firstname", None, "lastname"))
  val nino = Nino(nextNino)
  val personResidenceType = PersonResidenceType(UKResidence(nino), Some(Country("Spain", "ES")), Some(Country("Spain", "ES")))
  val updtdPersonResidenceType = PersonResidenceType(UKResidence(nino), Some(Country("France", "FR")), Some(Country("Spain", "ES")))
  val updtdPersonResidenceTypeYes = PersonResidenceType(UKResidence(nino), Some(Country("United Kingdom", "GB")), Some(Country("Spain", "ES")))
  val responsiblePeople = ResponsiblePerson(personName, personResidenceType = Some(personResidenceType))

  "CountryOfBirthController" when {

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePerson()

        when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controllers.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the country of birth page successfully with empty form" in new Fixture {

        val responsiblePeople = ResponsiblePerson(personName)

        when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controllers.get(RecordId)(request)
        status(result) must be(OK)

      }

      "display the country of birth page successfully with data from mongoCache" in new Fixture {

        when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = controllers.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("bornInUk-false").hasAttr("checked") must be(true)
        document.select("select[name=country] > option[value=ES]").hasAttr("selected") must be(true)
      }

      "display the country of birth page successfully with data from mongoCache for the option 'Yes'" in new Fixture {

        when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())
          (any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople.copy(personResidenceType = Some(updtdPersonResidenceTypeYes))))))

        val result = controllers.get(RecordId)(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("bornInUk-true").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "redirect to Nationality Controller" when {

        "all the mandatory inoput parameters are supplied" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "bornInUk" -> "false",
            "country" -> "FR"
          )
          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controllers.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())
            (any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controllers.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.NationalityController.get(RecordId).url))
          verify(controllers.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(),
            meq(Seq(responsiblePeople.copy(personResidenceType = Some(updtdPersonResidenceType), hasChanged = true))))(any(), any())
        }
      }

      "redirect to Detailed Answer Controller" when {

        "all the mandatory input parameters are supplied and in edit mode" in new Fixture {
          val requestWithParams = requestWithUrlEncodedBody(
            "bornInUk" -> "true"
          )
          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          when(controllers.dataCacheConnector.save[Seq[ResponsiblePerson]](any(), any(), any())
            (any(), any())).thenReturn(Future.successful(emptyCache))

          val result = controllers.post(RecordId, edit = true, Some(flowFromDeclaration))(requestWithParams)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, Some(flowFromDeclaration)).url))
          verify(controllers.dataCacheConnector).save[Seq[ResponsiblePerson]](any(), any(),
            meq(Seq(responsiblePeople.copy(personResidenceType = Some(updtdPersonResidenceTypeYes), hasChanged = true))))(any(), any())
        }
      }

      "respond with BAD_REQUEST" when {

        "bornInUk field is not supplied" in new Fixture {
          when(controllers.dataCacheConnector.fetch[Seq[ResponsiblePerson]](any(), meq(ResponsiblePerson.key))(any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))

          val line1MissingRequest = requestWithUrlEncodedBody("" -> "")

          val result = controllers.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)
        }

      }
    }
  }
}


