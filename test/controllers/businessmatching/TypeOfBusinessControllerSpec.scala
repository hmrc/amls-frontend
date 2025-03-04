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

package controllers.businessmatching

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.businessmatching.TypeOfBusinessFormProvider
import models.businessmatching.{BusinessMatching, TypeOfBusiness}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.cache.Cache
import utils.AmlsSpec
import views.html.businessmatching.TypeOfBusinessView

import scala.concurrent.Future

class TypeOfBusinessControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture {
    self =>
    val request                                       = addToken(authRequest)
    lazy val view: TypeOfBusinessView                 = app.injector.instanceOf[TypeOfBusinessView]
    lazy val formProvider: TypeOfBusinessFormProvider = app.injector.instanceOf[TypeOfBusinessFormProvider]
    val controller                                    = new TypeOfBusinessController(
      dataCacheConnector = mock[DataCacheConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = formProvider,
      view = view
    )
  }

  "TypeOfBusinessController" must {

    val emptyCache = Cache.empty

    "display business Types Page" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      val document  = Jsoup.parse(contentAsString(result))
      val pageTitle = messages("businessmatching.typeofbusiness.title") + " - " +
        messages("summary.businessmatching") + " - " +
        messages("title.amls") + " - " + messages("title.gov")
      document.title() mustBe pageTitle
    }

    "display main Summary Page" in new Fixture {

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(Some(BusinessMatching(typeOfBusiness = Some(TypeOfBusiness("test"))))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document  = Jsoup.parse(contentAsString(result))
      val pageTitle = messages("businessmatching.typeofbusiness.title") + " - " +
        messages("summary.businessmatching") + " - " +
        messages("title.amls") + " - " + messages("title.gov")
      document.title() mustBe pageTitle
      document.select("input[type=text]").`val`() must be("test")
    }

    "post with valid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.TypeOfBusinessController.post().url).withFormUrlEncodedBody(
        "typeOfBusiness" -> "text"
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.RegisterServicesController.get().url))
    }

    "post with valid data in edit mode" in new Fixture {

      val newRequest = FakeRequest(POST, routes.TypeOfBusinessController.post(true).url).withFormUrlEncodedBody(
        "typeOfBusiness" -> "text"
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))

    }

    "post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.TypeOfBusinessController.post().url).withFormUrlEncodedBody(
        "typeOfBusiness" -> "11" * 40
      )

      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.max.length.bm.businesstype.type"))

    }

    "post with missing mandatory field" in new Fixture {
      val newRequest = FakeRequest(POST, routes.TypeOfBusinessController.post().url).withFormUrlEncodedBody(
        "typeOfBusiness" -> ""
      )
      when(controller.dataCacheConnector.fetch[BusinessMatching](any(), any())(any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[BusinessMatching](any(), any(), any())(any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result)          must be(BAD_REQUEST)
      contentAsString(result) must include(messages("error.required.bm.businesstype.type"))
    }
  }
}
