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

package controllers.renewal

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.TotalThroughputFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.{ChequeCashingScrapMetal, CurrencyExchange, ForeignExchange, TransmittingMoney}
import models.businessmatching._
import models.moneyservicebusiness.ExpectedThroughput
import models.renewal._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.RenewalService
import services.cache.Cache
import utils.AmlsSpec
import views.html.renewal.TotalThroughputView

import scala.concurrent.Future

class TotalThroughputControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture {
    self =>
    implicit val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)
    val renewalService                                    = mock[RenewalService]
    val dataCacheConnector                                = mock[DataCacheConnector]
    val renewal                                           = Renewal()
    val cacheMap                                          = mock[Cache]
    lazy val view                                         = inject[TotalThroughputView]
    lazy val controller                                   = new TotalThroughputController(
      SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService,
      dataCacheConnector,
      cc = mockMcc,
      formProvider = inject[TotalThroughputFormProvider],
      view = view
    )
  }

  trait FormSubmissionFixture extends Fixture {
    self =>

    val formData    = "throughput" -> ExpectedThroughput.First.toString
    val formRequest = FakeRequest(POST, routes.TotalThroughputController.post().url).withFormUrlEncodedBody(formData)
    val cache       = mock[Cache]

    when {
      renewalService.updateRenewal(any(), any())
    } thenReturn Future.successful(cache)

    when {
      dataCacheConnector.fetchAll(any())
    } thenReturn Future.successful(Some(cache))

    setupBusinessMatching(Set(HighValueDealing, MoneyServiceBusiness))

    def post(edit: Boolean = false)(block: Future[Result] => Unit) =
      block(controller.post(edit)(formRequest))

    def setupBusinessMatching(activities: Set[BusinessActivity], msbServices: Set[BusinessMatchingMsbService] = Set()) =
      when {
        cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
      } thenReturn Some(
        BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(msbServices)),
          activities = Some(BusinessActivities(activities))
        )
      )
  }

  "The MSB throughput controller" must {
    "return the view with an empty form when no data exists yet" in new Fixture {

      when(renewalService.getRenewal(any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = contentAsString(result)
      html must include(Messages("renewal.msb.throughput.header"))

      val page = Jsoup.parse(html)

      ExpectedThroughput.all.map(_.toString) foreach { id =>
        page.getElementById(id).hasAttr("checked") must be(false)
      }
    }

    "return the view with prepopulated data" in new Fixture {

      when(renewalService.getRenewal(any()))
        .thenReturn(Future.successful(Some(Renewal(totalThroughput = Some(TotalThroughput("01"))))))

      val result = controller.get()(request)
      status(result) mustBe OK

      val page = Jsoup.parse(contentAsString(result))

      ExpectedThroughput.all.map(_.toString) foreach { id =>
        val result = if (id == "first") true else false
        page.getElementById(id).hasAttr("checked") must be(result)
      }
    }

    "return a bad request result when an invalid form is posted" in new Fixture {
      val result = controller.post()(request)
      status(result) mustBe BAD_REQUEST
    }
  }

  trait RenewalModelFormSubmissionFixture extends FormSubmissionFixture {
    val incomingModel = Renewal()
    val outgoingModel = incomingModel.copy(
      totalThroughput = Some(
        TotalThroughput(
          "01"
        )
      ),
      hasChanged = true
    )
    val newRequest    = FakeRequest(POST, routes.TotalThroughputController.post().url).withFormUrlEncodedBody(
      "throughput" -> ExpectedThroughput.First.toString
    )

    when(dataCacheConnector.fetchAll(any()))
      .thenReturn(Future.successful(Some(cacheMap)))

    when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
      .thenReturn(Some(incomingModel))

    when(dataCacheConnector.save[Renewal](any(), eqTo(Renewal.key), eqTo(outgoingModel))(any()))
      .thenReturn(Future.successful(Cache.empty))
  }

  "A valid form post to the MSB throughput controller" when {

    "edit is false" must {
      "redirect to TransactionsInLast12MonthsController if MSB MT" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing, MoneyServiceBusiness), Set(TransmittingMoney))

        post() { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.renewal.routes.TransactionsInLast12MonthsController.get().url
          )
        }
      }

      "redirect to CETransactionsInLast12MonthsController if MSB CE" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(CurrencyExchange))

        post() { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.renewal.routes.CETransactionsInLast12MonthsController.get().url
          )
        }
      }

      "redirect to FXTransactionsInLast12MonthsController if MSB FX" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(ForeignExchange))

        post() { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.renewal.routes.FXTransactionsInLast12MonthsController.get().url
          )
        }
      }

      "redirect to CustomersOutsideIsUK if HVD and ASP" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing, AccountancyServices), Set(ChequeCashingScrapMetal))

        post() { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url)
        }
      }

      "redirect to CustomersOutsideIsUK if HVD and NOT ASP" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(ChequeCashingScrapMetal))

        post() { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.renewal.routes.CustomersOutsideIsUKController.get().url)
        }
      }
    }

    "edit is true" must {
      "redirect to the summary page" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(ChequeCashingScrapMetal))

        post(edit = true) { result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.renewal.routes.SummaryController.get.url)
        }
      }
    }

    "save the throughput model into the renewals model when posted" in new RenewalModelFormSubmissionFixture {
      setupBusinessMatching(Set(HighValueDealing), Set(ChequeCashingScrapMetal))

      post() { result =>
        status(result) mustBe SEE_OTHER
        val captor = ArgumentCaptor.forClass(classOf[Renewal])
        verify(renewalService).updateRenewal(any(), captor.capture())
        captor.getValue.totalThroughput mustBe Some(TotalThroughput("01"))
      }
    }
  }
}
