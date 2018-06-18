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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import controllers.businessmatching.updateservice.ChangeSubSectorHelper
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.{ChangeSubSectorFlowModel, SubSectorsPageId}
import models.moneyservicebusiness.{MoneyServiceBusiness, MoneyServiceBusinessTestData}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MsbSubSectorsControllerSpec extends AmlsSpec with ScalaFutures with MoneyServiceBusinessTestData with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val request = addToken(authRequest)

    val controller = new MsbSubSectorsController(
      self.authConnector,
      mockCacheConnector,
      createRouter[ChangeSubSectorFlowModel],
      mock[BusinessMatchingService],
      mock[ChangeSubSectorHelper]
    )

    val cacheMapT = OptionT.some[Future, CacheMap](mockCacheMap)

    when {
      controller.businessMatchingService.updateModel(any())(any(), any(), any())
    } thenReturn cacheMapT

    when {
      controller.helper.updateSubSectors(any())(any(), any(), any())
    } thenReturn Future.successful(mock[MoneyServiceBusiness], mock[BusinessMatching], Seq.empty)

    def setupModel(model: Option[BusinessMatching]): Unit = when {
      controller.businessMatchingService.getModel(any(), any(), any())
    } thenReturn (model match {
      case Some(bm) => OptionT.pure[Future, BusinessMatching](bm)
      case _ => OptionT.none[Future, BusinessMatching]
    })
  }

  "ServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {

      setupModel(None)

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.select(".amls-error-summary").size mustBe 0
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

    "redirect to the 'PSR Number' page on valid submission when adding 'Transmitting Money' and you don't have a PSR Number" in new Fixture {

      mockCacheUpdate[ChangeSubSectorFlowModel](Some(ChangeSubSectorFlowModel.key), ChangeSubSectorFlowModel())

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01"
      )

      val result = controller.post()(newRequest)

      status(result) mustBe SEE_OTHER

      controller.router.verify(SubSectorsPageId, ChangeSubSectorFlowModel(Some(Set(TransmittingMoney))))
    }

    "redirect to the summary page when adding 'CurrencyExchange' as a service" in new Fixture {

      mockCacheUpdate[ChangeSubSectorFlowModel](Some(ChangeSubSectorFlowModel.key), ChangeSubSectorFlowModel(Some(Set(ChequeCashingNotScrapMetal))))

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      val result = controller.post()(newRequest)

      status(result) mustBe SEE_OTHER

      controller.router.verify(SubSectorsPageId, ChangeSubSectorFlowModel(Some(Set(CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal))))

    }
  }
}