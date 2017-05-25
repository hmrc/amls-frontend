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

package controllers.businessmatching

import connectors.DataCacheConnector
import models.businessmatching._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.scalatest.mock.MockitoSugar
import utils.{GenericTestHelper, AuthorisedFixture}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class ServicesControllerSpec extends GenericTestHelper with ScalaFutures with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new ServicesController {
      override def cache: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "ServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.select(".amls-error-summary").size mustBe 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = BusinessMatching(
        msbServices = Some(
          MsbServices(Set(TransmittingMoney, CurrencyExchange))
        )
      )

      when(cache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox][checked]").size mustBe 2
      document.select("input[value=01]").hasAttr("checked") mustBe true
      document.select("input[value=02]").hasAttr("checked") mustBe true
      document.select(".amls-error-summary").size mustBe 0
    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "invalid"
      )

      val result = controller.post()(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST

      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
    }

    "return a redirect to the 'How much Throughput' page on valid submission" in new Fixture {

      val model = BusinessMatching(
        msbServices = Some(MsbServices(
          Set(TransmittingMoney)
        )),
        businessAppliedForPSRNumber = None,
        hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01"
      )

      when(cache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(cache.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.BusinessAppliedForPSRNumberController.get(false).url)
    }

    "return a redirect to the 'X' page when adding 'Transmitting Money' as a service during edit" in new Fixture {

      val currentModel = BusinessMatching(
        msbServices = Some(MsbServices(
          Set(ChequeCashingNotScrapMetal)
        ))
      )

      val newModel = currentModel.copy(
        msbServices = Some(MsbServices(
          Set(TransmittingMoney, CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
        )), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01",
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      when(cache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(currentModel)))

      when(cache.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.BusinessAppliedForPSRNumberController.get(true).url)
    }

    "return a redirect to the 'X' page when adding 'CurrencyExchange' as a service during edit" in new Fixture {

      val currentModel = BusinessMatching(
        msbServices = Some(MsbServices(
          Set(ChequeCashingNotScrapMetal)
        ))
      )

      val newModel = currentModel.copy(
        msbServices = Some(MsbServices(
          Set(CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
        )), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      when(cache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(currentModel)))

      when(cache.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.get().url)
    }

    "return a redirect to the 'Check Your Answers' page when adding 'Cheque Cashing' as a service during edit" in new Fixture {

      Seq[(MsbService, String)]((ChequeCashingNotScrapMetal, "03"), (ChequeCashingScrapMetal, "04")) foreach {
        case (model, id) =>
          val currentModel = BusinessMatching(
            msbServices = Some(MsbServices(
              Set(TransmittingMoney, CurrencyExchange)
            ))
          )

          val newModel = currentModel.copy(
            msbServices = Some(MsbServices(
              Set(TransmittingMoney, CurrencyExchange, model)
            )), hasChanged = true
          )

          val newRequest = request.withFormUrlEncodedBody(
            "msbServices[1]" -> "01",
            "msbServices[2]" -> "02",
            "msbServices[3]" -> id
          )

          when(cache.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
            .thenReturn(Future.successful(Some(currentModel)))

          when(cache.save[BusinessMatching](eqTo(BusinessMatching.key), any())(any(), any(), any()))
            .thenReturn(Future.successful(new CacheMap("", Map.empty)))

          val result = controller.post(edit = true)(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAppliedForPSRNumberController.get(true).url)
      }
    }
  }
}
