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

package controllers.renewal

import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities, _}
import models.renewal._
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future


class TotalThroughputControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    implicit val request = addToken(authRequest)
    val renewalService = mock[RenewalService]
    val dataCacheConnector = mock[DataCacheConnector]
    val renewal = Renewal()
    val cacheMap = mock[CacheMap]

    lazy val controller = new TotalThroughputController(
      self.authConnector,
      renewalService,
      dataCacheConnector
    )
  }

  trait FormSubmissionFixture extends Fixture {
    self =>

    val formData = "throughput" -> "01"
    val formRequest = request.withFormUrlEncodedBody(formData)
    val cache = mock[CacheMap]

    when {
      renewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(cache)

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))

    setupBusinessMatching(Set(HighValueDealing, MoneyServiceBusiness))

    def post(edit: Boolean = false)(block: Result => Unit) =
      block(await(controller.post(edit)(formRequest)))

    def setupBusinessMatching(activities: Set[BusinessActivity], msbServices: Set[BusinessMatchingMsbService] = Set()) = when {
        cacheMap.getEntry[BusinessMatching](BusinessMatching.key)
    } thenReturn Some(BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(msbServices)), activities = Some(BusinessActivities(activities))))
  }

  "The MSB throughput controller" must {
    "return the view with an empty form when no data exists yet" in new Fixture {

      when(renewalService.getRenewal(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)

      status(result) mustBe OK

      val html = contentAsString(result)
      html must include(Messages("renewal.msb.throughput.header"))

      val page = Jsoup.parse(html)

      page.select("input[type=radio][name=throughput][id=throughput-01]").hasAttr("checked") must be(false)
      page.select("input[type=radio][name=throughput][id=throughput-02]").hasAttr("checked") must be(false)
      page.select("input[type=radio][name=throughput][id=throughput-03]").hasAttr("checked") must be(false)
      page.select("input[type=radio][name=throughput][id=throughput-04]").hasAttr("checked") must be(false)
      page.select("input[type=radio][name=throughput][id=throughput-05]").hasAttr("checked") must be(false)
      page.select("input[type=radio][name=throughput][id=throughput-06]").hasAttr("checked") must be(false)
      page.select("input[type=radio][name=throughput][id=throughput-07]").hasAttr("checked") must be(false)
    }

    "return the view with prepopulated data" in new Fixture {

      when(renewalService.getRenewal(any(), any(), any()))
        .thenReturn(Future.successful(Some(Renewal(totalThroughput = Some(TotalThroughput("01"))))))

      val result = controller.get()(request)
      status(result) mustBe OK

      val page = Jsoup.parse(contentAsString(result))
      page.select("input[type=radio][name=throughput][id=throughput-01]").hasAttr("checked") must be(true)
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
          ), hasChanged = true
      )
      val newRequest = request.withFormUrlEncodedBody(
          "throughput" -> "01"
      )

      when(dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(cacheMap)))

      when(cacheMap.getEntry[Renewal](eqTo(Renewal.key))(any()))
              .thenReturn(Some(incomingModel))

      when(dataCacheConnector.save[Renewal](eqTo(Renewal.key), eqTo(outgoingModel))(any(), any(), any()))
              .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "A valid form post to the MSB throughput controller" when {

    "edit is false" must {
      "redirect to TransactionsInLast12MonthsController if MSB MT" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing, MoneyServiceBusiness), Set(TransmittingMoney))

        post() { result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.TransactionsInLast12MonthsController.get().url)
        }
      }

      "redirect to CETransactionsInLast12MonthsController if MSB CE" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(CurrencyExchange))

        post() { result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.CETransactionsInLast12MonthsController.get().url)
        }
      }

      "redirect to FXTransactionsInLast12MonthsController if MSB FX" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(ForeignExchange))

        post() { result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.FXTransactionsInLast12MonthsController.get().url)
        }
      }

      "redirect to PercentageOfCashPaymentOver15000Controller if HVD and ASP" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing, AccountancyServices), Set(ChequeCashingScrapMetal))

        post() { result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get().url)
        }
      }

      "redirect to CustomersOutsideUK if HVD and NOT ASP" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(ChequeCashingScrapMetal))

        post() { result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.CustomersOutsideUKController.get().url)
        }
      }
    }

    "edit is true" must {
      "redirect to the summary page" in new RenewalModelFormSubmissionFixture {
        setupBusinessMatching(Set(HighValueDealing), Set(ChequeCashingScrapMetal))

        post(edit = true) { result =>
          result.header.status mustBe SEE_OTHER
          result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.SummaryController.get().url)
        }
      }
    }

    "save the throughput model into the renewals model when posted" in new RenewalModelFormSubmissionFixture {
      setupBusinessMatching(Set(HighValueDealing), Set(ChequeCashingScrapMetal))

      post() { result =>
        result.header.status mustBe SEE_OTHER
        val captor = ArgumentCaptor.forClass(classOf[Renewal])
        verify(renewalService).updateRenewal(captor.capture())(any(), any(), any())
        captor.getValue.totalThroughput mustBe Some(TotalThroughput("01"))
      }
    }
  }
}
