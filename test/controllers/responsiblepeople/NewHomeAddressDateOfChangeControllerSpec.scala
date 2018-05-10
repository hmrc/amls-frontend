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
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.mockito.Matchers._
import org.mockito.Mockito.{when, _}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import play.api.test.Helpers._
import utils.{AuthorisedFixture, AmlsSpec}

import scala.concurrent.Future

class NewHomeAddressDateOfChangeControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)
    val dataCacheConnector = mock[DataCacheConnector]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .build()

    val controller = app.injector.instanceOf[NewHomeAddressDateOfChangeController]

    val cacheMap = mock[CacheMap]
  }

  "NewHomeAddressDateOfChangeController" must {

    val responsiblePeople = ResponsiblePeople(personName = Some(PersonName("FirstName",
      None, "lastName")),
      positions = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2009,1,1)))))

    "Get:" must {
      "successfully load when the person moved to new address page" when {
        "date of change model exists in save4later" in new Fixture {

          when(cacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))
          when(cacheMap.getEntry[NewHomeDateOfChange](NewHomeDateOfChange.key)).thenReturn(Some(NewHomeDateOfChange(Some(LocalDate.now()))))
          when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(cacheMap)))

          val result = controller.get(1)(request)
          status(result) must be(OK)

        }

        "date of change model in not persisted in save4later" in new Fixture {

          when(cacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(Some(Seq(responsiblePeople)))
          when(cacheMap.getEntry[NewHomeDateOfChange](NewHomeDateOfChange.key)).thenReturn(None)
          when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(cacheMap)))

          val result = controller.get(1)(request)
          status(result) must be(OK)
        }

        "load NotFound view when index is not in range" in new Fixture {
          when(cacheMap.getEntry[Seq[ResponsiblePeople]](any())(any()))
            .thenReturn(None)
          when(cacheMap.getEntry[NewHomeDateOfChange](NewHomeDateOfChange.key)).thenReturn(None)
          when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(cacheMap)))

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "Post:" must {
      "redirect to next page successfully for valid input" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.day" -> "20",
          "dateOfChange.month" -> "5",
          "dateOfChange.year" -> "2014"
        )
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        when(controller.dataCacheConnector.save[NewHomeDateOfChange](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(cacheMap))
        val result = controller.post(1)(postRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.NewHomeAddressController.get(1).url))
      }

      "fail validation on invalid input" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01"
        )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(controller.dataCacheConnector.save[NewHomeDateOfChange](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(cacheMap))

        val result = controller.post(1)(postRequest)

        status(result) must be(BAD_REQUEST)

      }

      "fail validation when input data is before activity start date" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01"
        )

        val position = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2011,1,1))))
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(responsiblePeople.copy(positions = position)))))
        when(controller.dataCacheConnector.save[NewHomeDateOfChange](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(cacheMap))

        val result = controller.post(1)(postRequest)

        status(result) must be(BAD_REQUEST)
      }

      "redirect to NotFound when index is out of range" in new Fixture {
        val postRequest = request.withFormUrlEncodedBody(
          "dateOfChange.year" -> "2010",
          "dateOfChange.month" -> "10",
          "dateOfChange.day" -> "01"
        )

        val position = Some(Positions(Set(BeneficialOwner),Some(new LocalDate(2011,1,1))))
        when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))
        when(controller.dataCacheConnector.save[NewHomeDateOfChange](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(cacheMap))

        val result = controller.post(1)(postRequest)

        status(result) must be(NOT_FOUND)
      }

    }

  }
}
