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

import cats.data.OptionT
import config.ApplicationConfig
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import forms.businessmatching.MsbSubSectorsFormProvider
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.businessmatching.BusinessMatchingMsbService._
import models.flowmanagement.{ChangeSubSectorFlowModel, SubSectorsPageId}
import models.moneyservicebusiness.{MoneyServiceBusiness, MoneyServiceBusinessTestData}
import models.status.NotCompleted
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.MsbServicesView

import scala.concurrent.Future

class MsbSubSectorsControllerSpec extends AmlsSpec with ScalaFutures with MoneyServiceBusinessTestData with BusinessMatchingGenerator {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val config = mock[ApplicationConfig]
    lazy val view = app.injector.instanceOf[MsbServicesView]
    val controller = new MsbSubSectorsController(
      SuccessfulAuthAction, ds = commonDependencies,
      mockCacheConnector,
      createRouter2[ChangeSubSectorFlowModel],
      mock[BusinessMatchingService],
      mockStatusService,
      mock[ChangeSubSectorHelper],
      config,
      cc = mockMcc,
      formProvider = app.injector.instanceOf[MsbSubSectorsFormProvider],
      services = view
    )

    val cacheMapT = OptionT.liftF[Future, Cache](Future.successful(mockCacheMap))

    when {
      controller.businessMatchingService.updateModel(any(), any())(any())
    } thenReturn cacheMapT

    when {
      controller.helper.updateSubSectors(any(), any())(any())
    } thenReturn Future.successful((mock[MoneyServiceBusiness], mock[BusinessMatching], Seq.empty))

    def setupModel(model: Option[BusinessMatching]): Unit = when {
      controller.businessMatchingService.getModel(any())
    } thenReturn (model match {
      case Some(bm) => OptionT.liftF[Future, BusinessMatching](Future.successful(bm))
      case _ => OptionT.none[Future, BusinessMatching]
    })

    mockApplicationStatus(NotCompleted)
  }

  "ServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {

      setupModel(None)

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.getElementsByClass("govuk-list govuk-error-summary__list").size mustBe 0
    }

    "show an empty form on get with no data in store when fx enabled" in new Fixture {

      when(config.fxEnabledToggle) thenReturn true

      setupModel(None)

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox]").size mustBe 5
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.getElementsByClass("govuk-list govuk-error-summary__list").size mustBe 0
    }

    "show a pre-filled form when there is data in the store" in new Fixture {

      val model = BusinessMatching(
        msbServices = Some(
          BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))
        )
      )

      setupModel(Some(model))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox][checked]").size mustBe 2
      document.select(s"input[value=${TransmittingMoney.toString}]").hasAttr("checked") mustBe true
      document.select(s"input[value=${CurrencyExchange.toString}]").hasAttr("checked") mustBe true
      document.getElementsByClass("govuk-list govuk-error-summary__list").size mustBe 0
    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = FakeRequest(POST, routes.MsbSubSectorsController.post().url)
        .withFormUrlEncodedBody("value[1]" -> "")

      val result = controller.post()(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST

      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
    }

    "return a Bad Request with errors on invalid submission when fx enabled" in new Fixture {

      when(config.fxEnabledToggle) thenReturn true

      val newRequest = FakeRequest(POST, routes.MsbSubSectorsController.post().url)
        .withFormUrlEncodedBody("value[1]" -> "")

      val result = controller.post()(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST

      document.select("input[type=checkbox]").size mustBe 5
      document.select("input[type=checkbox][checked]").size mustBe 0
    }

    "redirect to the 'PSR Number' page on valid submission when adding 'Transmitting Money' and you don't have a PSR Number" in new Fixture {

      mockCacheUpdate[ChangeSubSectorFlowModel](Some(ChangeSubSectorFlowModel.key), ChangeSubSectorFlowModel())

      val newRequest = FakeRequest(POST, routes.MsbSubSectorsController.post().url)
        .withFormUrlEncodedBody(
          "value[1]" -> TransmittingMoney.toString
      )

      val result = controller.post()(newRequest)

      status(result) mustBe SEE_OTHER

      controller.router.verify("internalId", SubSectorsPageId, ChangeSubSectorFlowModel(Some(Set(TransmittingMoney))))
    }

    "redirect to the summary page when adding anything other than TransmittingMoney as a service" in new Fixture {

      mockCacheUpdate[ChangeSubSectorFlowModel](Some(ChangeSubSectorFlowModel.key), ChangeSubSectorFlowModel(Some(Set(ChequeCashingNotScrapMetal))))

      val newRequest = FakeRequest(POST, routes.MsbSubSectorsController.post().url)
        .withFormUrlEncodedBody(
          "value[1]" -> CurrencyExchange.toString,
          "value[2]" -> ChequeCashingNotScrapMetal.toString,
          "value[3]" -> ChequeCashingScrapMetal.toString,
          "value[4]" -> ForeignExchange.toString
      )

      val result = controller.post()(newRequest)

      status(result) mustBe SEE_OTHER

      controller.router.verify("internalId", SubSectorsPageId, ChangeSubSectorFlowModel(Some(Set(CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal, ForeignExchange))))

    }
  }
}