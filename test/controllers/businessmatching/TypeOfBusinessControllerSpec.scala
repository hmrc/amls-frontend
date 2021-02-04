/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.businessmatching

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.businessmatching.{BusinessMatching, TypeOfBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AmlsSpec
import views.html.businessmatching.type_of_business

import scala.concurrent.Future

class TypeOfBusinessControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    self => val request = addToken(authRequest)
    lazy val view = app.injector.instanceOf[type_of_business]
    val controller = new TypeOfBusinessController (
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      type_of_business = view)
  }

  "TypeOfBusinessController" must {

    val emptyCache = CacheMap("", Map.empty)

    "display business Types Page" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      val pageTitle = Messages("businessmatching.typeofbusiness.title") + " - " +
        Messages("summary.businessmatching") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")
      document.title() mustBe pageTitle
    }

    "display main Summary Page" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any(), any())).thenReturn(
        Future.successful(Some(BusinessMatching(typeOfBusiness = Some(TypeOfBusiness("test"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      val pageTitle = Messages("businessmatching.typeofbusiness.title") + " - " +
        Messages("summary.businessmatching") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")
      document.title() mustBe pageTitle
      document.select("input[type=text]").`val`() must be("test")
    }

    "post with valid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "typeOfBusiness" -> "text"
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))
    }

    "post with valid data in edit mode" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "typeOfBusiness" -> "text"
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))

    }

    "post with invalid data" in new Fixture {

      val newRequest = requestWithUrlEncodedBody(
        "typeOfBusiness" -> "11"*40
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.max.length.bm.businesstype.type"))

    }

    "post with missing mandatory field" in new Fixture {
      val newRequest = requestWithUrlEncodedBody(
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())
        (any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())
        (any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required"))
    }
  }
}
