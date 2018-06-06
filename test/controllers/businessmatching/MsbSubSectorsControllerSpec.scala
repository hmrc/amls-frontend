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
import connectors.DataCacheConnector
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.moneyservicebusiness.{MoneyServiceBusiness, MoneyServiceBusinessTestData}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, AmlsSpec}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class MsbSubSectorsControllerSpec extends AmlsSpec with ScalaFutures with MockitoSugar with MoneyServiceBusinessTestData with BusinessMatchingGenerator {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new MsbSubSectorsController {
      override def dataCacheConnector: DataCacheConnector = self.cache
      override lazy val businessMatchingService = mock[BusinessMatchingService]
      override protected def authConnector: AuthConnector = self.authConnector
    }

    val mockCacheMap = mock[CacheMap]
    val cacheMapT = OptionT.some[Future, CacheMap](mockCacheMap)

    when {
      controller.dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(mockCacheMap))

    when {
      controller.businessMatchingService.updateModel(any())(any(), any(), any())
    } thenReturn cacheMapT

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

    "show a prefilled form when there is data in the store" in new Fixture {

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

    "redirect to the 'How much Throughput' page on valid submission" in new Fixture {

      val model = BusinessMatching(
        msbServices = Some(BusinessMatchingMsbServices(
          Set(TransmittingMoney)
        )),
        businessAppliedForPSRNumber = None,
        hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01"
      )

      setupModel(Some(model))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(completeMsb))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post()(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.BusinessAppliedForPSRNumberController.get().url)
    }

    "redirect to the Psr Number page when adding 'Transmitting Money' as a service during edit" in new Fixture {

      val currentModel = BusinessMatching(
        msbServices = Some(BusinessMatchingMsbServices(
          Set(ChequeCashingNotScrapMetal)
        ))
      )

      val newModel = currentModel.copy(
        msbServices = Some(BusinessMatchingMsbServices(
          Set(TransmittingMoney, CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
        )), hasChanged = true
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01",
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      setupModel(Some(currentModel))

      when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
        .thenReturn(Some(completeMsb))

      when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
        .thenReturn(Future.successful(mockCacheMap))

      val result = controller.post(edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.BusinessAppliedForPSRNumberController.get(true).url)
    }

    "redirect to the summary page when adding 'CurrencyExchange' as a service during edit" in new Fixture {

        val currentModel = BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(
            Set(ChequeCashingNotScrapMetal)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[1]" -> "02",
          "msbServices[2]" -> "03",
          "msbServices[3]" -> "04"
        )

        setupModel(Some(currentModel))

        when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
          .thenReturn(Some(completeMsb))

        when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SummaryController.get().url)

      }

    "redirect to the 'Psr Number' page when adding 'Cheque Cashing' as a service during edit" in new Fixture {

      Seq[String]("03", "04") foreach {
        case (id) =>

          val currentModel = BusinessMatching(
            msbServices = Some(BusinessMatchingMsbServices(
              Set(TransmittingMoney)
            ))
          )

          val newRequest = request.withFormUrlEncodedBody(
            "msbServices[0]" -> "01",
            "msbServices[1]" -> id
          )

          setupModel(Some(currentModel))

          when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
            .thenReturn(Some(completeMsb))

          when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
            .thenReturn(Future.successful(mockCacheMap))

          val result = controller.post(edit = true)(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.BusinessAppliedForPSRNumberController.get(true).url)
      }
    }
  }

  it must {
    "remove existing MSB Transmitting Money data" when {
      "Transmitting Money is no longer present in selection" in new Fixture {

        val currentModel = BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(
            Set(ChequeCashingNotScrapMetal, TransmittingMoney)
          ))
        )

        val newModel = currentModel.copy(
          msbServices = Some(BusinessMatchingMsbServices(
            Set(ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
          )), hasChanged = true
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "03",
          "msbServices[1]" -> "04"
        )

        setupModel(Some(currentModel))

        when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
          .thenReturn(Some(completeMsb))

        when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(edit = true)(newRequest)

        status(result) mustBe SEE_OTHER

        verify(controller.dataCacheConnector).save[MoneyServiceBusiness](any(), eqTo(completeMsb.copy(
          businessUseAnIPSP = None,
          fundsTransfer = None,
          transactionsInNext12Months = None,
          sendMoneyToOtherCountry = None,
          sendTheLargestAmountsOfMoney = None,
          mostTransactions = None
        )))(any(), any(), any())

      }
    }

    "remove existing MSB Currency Exchange data" when {
      "Currency Exchange is no longer present in selection" in new Fixture {

        val currentModel = BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(
            Set(CurrencyExchange, ChequeCashingNotScrapMetal)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "03",
          "msbServices[1]" -> "04"
        )

        setupModel(Some(currentModel))

        when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
          .thenReturn(Some(completeMsb))

        when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(edit = true)(newRequest)

        status(result) mustBe SEE_OTHER

        verify(controller.dataCacheConnector).save[MoneyServiceBusiness](any(), eqTo(completeMsb.copy(
          ceTransactionsInNext12Months = None,
          whichCurrencies = None
        )))(any(), any(), any())

      }
    }

    "save same MSB data as fetched" when {
      "Transmitting Money or Currency Exchange was not in existing MSB services" in new Fixture {

        val currentModel = BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(
            Set(ChequeCashingNotScrapMetal)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "03",
          "msbServices[1]" -> "04"
        )

        setupModel(Some(currentModel))

        when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
          .thenReturn(Some(completeMsb))

        when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(edit = true)(newRequest)

        status(result) mustBe SEE_OTHER

        verify(controller.dataCacheConnector).save[MoneyServiceBusiness](any(), eqTo(completeMsb))(any(), any(), any())

      }

      "Transmitting Money or Currency Exchange remains in updated MSB services" in new Fixture {

        val currentModel = BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(
            Set(TransmittingMoney, CurrencyExchange, ChequeCashingNotScrapMetal)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "01",
          "msbServices[1]" -> "02",
          "msbServices[2]" -> "04"
        )

        setupModel(Some(currentModel))

        when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
          .thenReturn(Some(completeMsb))

        when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post(edit = true)(newRequest)

        status(result) mustBe SEE_OTHER

        verify(controller.dataCacheConnector).save[MoneyServiceBusiness](any(), eqTo(completeMsb))(any(), any(), any())

      }
    }

    "carry on to redirect" when {
      "MSB data does not exist" in new Fixture {

        val currentModel = BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(
            Set(ChequeCashingNotScrapMetal)
          ))
        )

        val newRequest = request.withFormUrlEncodedBody(
          "msbServices[0]" -> "03",
          "msbServices[1]" -> "04"
        )

        setupModel(Some(currentModel))

        when(mockCacheMap.getEntry[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any()))
          .thenReturn(None)

        when(cache.save[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key), any())(any(), any(), any()))
          .thenReturn(Future.successful(mockCacheMap))

        val result = controller.post()(newRequest)

        status(result) mustBe SEE_OTHER

      }
    }

  }

}