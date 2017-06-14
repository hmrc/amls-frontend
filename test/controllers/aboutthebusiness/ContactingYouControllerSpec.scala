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

package controllers.aboutthebusiness

import java.util.UUID

import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, ContactingYou}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ContactingYouControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val contactingYou = Some(ContactingYou(Some("+44 (0)123 456-7890"), Some("test@test.com")))
  val aboutTheBusinessWithData = AboutTheBusiness(contactingYou = contactingYou)

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ContactingYouController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessHasEmailController" must {

    "Get" must {

      "load the page" in new Fixture {

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.email.title"))
      }

      "load the page with the pre populated data" in new Fixture {

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.email.title"))
      }

      "load the page with no data" in new Fixture {
        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.email.title"))
      }

    }

    "Post" must {

      "on post of valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "confirmEmail" -> "test@test.com",
          "email" -> "test@test.com"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.ContactingYouPhoneController.get().url))
      }


      "on post of incomplete data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "email" -> "test@test.com"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }

      "on post of different email addresses" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "confirmEmail" -> "test@test.com",
          "email" -> "test1@test.com"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }






    }
  }

}