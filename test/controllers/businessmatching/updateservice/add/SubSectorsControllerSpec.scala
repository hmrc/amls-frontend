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

package controllers.businessmatching.updateservice.add

import cats.data.OptionT
import cats.implicits._
import config.ApplicationConfig
import controllers.actions.SuccessfulAuthAction
import controllers.businessmatching.updateservice.AddBusinessTypeHelper
import generators.businessmatching.BusinessMatchingGenerator
import models.businessmatching._
import models.flowmanagement.{AddBusinessTypeFlowModel, SubSectorsPageId}
import models.moneyservicebusiness.MoneyServiceBusinessTestData
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, DependencyMocks}
import views.html.businessmatching.updateservice.add.msb_subservices


import scala.concurrent.Future

class SubSectorsControllerSpec extends AmlsSpec with MoneyServiceBusinessTestData with BusinessMatchingGenerator {

  sealed trait Fixture extends DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val config = mock[ApplicationConfig]

    val mockBusinessMatchingService = mock[BusinessMatchingService]
    val mockUpdateServiceHelper = mock[AddBusinessTypeHelper]
    lazy val view = app.injector.instanceOf[msb_subservices]
    val controller = new SubSectorsController(
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      dataCacheConnector = mockCacheConnector,
      businessMatchingService = mockBusinessMatchingService,
      router = createRouter[AddBusinessTypeFlowModel],
      config = config,
      cc = mockMcc,
      msb_subservices = view
    )


    val cacheMapT = OptionT.some[Future, CacheMap](mockCacheMap)

    when {
      controller.businessMatchingService.getModel(any())(any())
    } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(
      activities = Some(BusinessActivities(Set(AccountancyServices)))
    ))

    when {
      controller.businessMatchingService.updateModel(any(), any())(any(), any())
    } thenReturn cacheMapT
  }


  "SubServicesController" when {

    "get is called" must {
      "return OK with 'msb_subservices' populated view and FXtoggle is enabled" in new Fixture {

        when(config.fxEnabledToggle) thenReturn true
        mockCacheFetch(Some(AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
          subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))))))
        val result = controller.get()(request)

        status(result) must be(OK)

        contentAsString(result) must include(Messages("businessmatching.updateservice.msb.services.heading"))
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[type=checkbox]").size mustBe 5
        document.select("input[type=checkbox][checked]").size mustBe 2
        document.select(".amls-error-summary").size mustBe 0

      }

      "return OK with 'msb_subservices' populated view and FXtoggle is not enabled" in new Fixture {

        when(config.fxEnabledToggle) thenReturn false
        mockCacheFetch(Some(AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
          subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))))))
        val result = controller.get()(request)

        status(result) must be(OK)

        contentAsString(result) must include(Messages("businessmatching.updateservice.msb.services.heading"))
        val document = Jsoup.parse(contentAsString(result))
        document.select("input[type=checkbox]").size mustBe 4
        document.select("input[type=checkbox][checked]").size mustBe 2
        document.select(".amls-error-summary").size mustBe 0

      }
    }

    "return OK with a 'msb_subservices' not populated view and FXtoggle is not enabled" in new Fixture {

      when(config.fxEnabledToggle) thenReturn false
      mockCacheFetch(Some(AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness), subSectors = None)))
      val result = controller.get()(request)

      status(result) must be(OK)

      contentAsString(result) must include(Messages("businessmatching.updateservice.msb.services.heading"))
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[type=checkbox]").size mustBe 4
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.select(".amls-error-summary").size mustBe 0
    }

    "return OK with a 'msb_subservices' not populated view and FXtoggle is enabled" in new Fixture {

      when(config.fxEnabledToggle) thenReturn true
      mockCacheFetch(Some(AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness), subSectors = None)))
      val result = controller.get()(request)

      status(result) must be(OK)
      contentAsString(result) must include(Messages("businessmatching.updateservice.msb.services.heading"))
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[type=checkbox]").size mustBe 5
      document.select("input[type=checkbox][checked]").size mustBe 0
      document.select(".amls-error-summary").size mustBe 0
    }


    "post is called" must {

      "return a bad request when no data has been posted" in new Fixture {

        val result = controller.post()(requestWithUrlEncodedBody("" -> ""))

        status(result) mustBe BAD_REQUEST
      }

      "return the 'Does Your Business ... PSR ...' page in the flow" when {
        "money transfer has been posted" in new Fixture {
          mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
            hasChanged = true))

          val result = controller.post()(requestWithUrlEncodedBody(
            "msbServices[]" -> "01"
          ))

          status(result) mustBe SEE_OTHER
          controller.router.verify("internalId", SubSectorsPageId,
            AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
              subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
              hasChanged = true))
        }
      }

      "return the 'Responsible people' page in the flow" when {
        "anything other than money transfer has been posted" in new Fixture {
          mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
            hasChanged = true)
          )

          val result = controller.post()(requestWithUrlEncodedBody(
            "msbServices[]" -> "03",
            "msbServices[]" -> "04"
          ))

          status(result) mustBe SEE_OTHER
          controller.router.verify("internalId", SubSectorsPageId,
            AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
              subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))),
              hasChanged = true))
        }
      }

      "return the 'Responsible people' page in the flow" when {
        "anything other than money transfer has been posted when editing existing full flow with only money transfer" in new Fixture {
          mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))),
            hasChanged = true)
          )

          val result = controller.post()(requestWithUrlEncodedBody(
            "msbServices[]" -> "03",
            "msbServices[]" -> "04"
          ))

          status(result) mustBe SEE_OTHER
          controller.router.verify("internalId", SubSectorsPageId,
            AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
              subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))),
              hasChanged = true))
        }
      }

      "return the 'Responsible people' page in the flow" when {
        "anything other than money transfer has been posted when editing existing full flow with money transfer and others" in new Fixture {
          mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingScrapMetal))),
            hasChanged = true)
          )

          val result = controller.post()(requestWithUrlEncodedBody(
            "msbServices[]" -> "03",
            "msbServices[]" -> "04"
          ))

          status(result) mustBe SEE_OTHER
          controller.router.verify("internalId", SubSectorsPageId,
            AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
              subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))),
              hasChanged = true))
        }
      }

      "return the 'Responsible people' page in the flow" when {
        "anything other than money transfer has been posted when completely changing selected services" in new Fixture {
          mockCacheUpdate(Some(AddBusinessTypeFlowModel.key), AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
            subSectors = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))),
            hasChanged = true)
          )

          val result = controller.post()(requestWithUrlEncodedBody(
            "msbServices[]" -> "03",
            "msbServices[]" -> "04"
          ))

          status(result) mustBe SEE_OTHER
          controller.router.verify("internalId", SubSectorsPageId,
            AddBusinessTypeFlowModel(activity = Some(MoneyServiceBusiness),
              subSectors = Some(BusinessMatchingMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal))),
              hasChanged = true))
        }
      }
    }
  }
}