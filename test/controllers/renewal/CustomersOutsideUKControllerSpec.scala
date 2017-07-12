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

package controllers.renewal

import connectors.DataCacheConnector
import models.Country
import models.businessmatching.{BusinessActivities, BusinessMatching, HighValueDealing, MoneyServiceBusiness}
import models.renewal.{CustomersOutsideUK, Renewal}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class CustomersOutsideUKControllerSpec extends GenericTestHelper {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    implicit val authContext = mock[AuthContext]
    implicit val headerCarrier = HeaderCarrier()

    val dataCacheConnector = mock[DataCacheConnector]
    val renewalService = mock[RenewalService]

    val emptyCache = CacheMap("", Map.empty)
    val mockCacheMap = mock[CacheMap]

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[DataCacheConnector].to(dataCacheConnector))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[RenewalService].to(renewalService))
      .build()

    val controller = app.injector.instanceOf[CustomersOutsideUKController]

  }

  trait FormSubmissionFixture extends Fixture {
    def formData(valid: Boolean) = if (valid) {
      "isOutside" -> "false"
    } else {
      "isOutside" -> "abc"
    }

    def formRequest(valid: Boolean) = request.withFormUrlEncodedBody(formData(valid))

    val cache = mock[CacheMap]

    when {
      renewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(cache)

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))

    when(cache.getEntry[Renewal](Renewal.key))
      .thenReturn(Some(Renewal(
        customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("GB", "GB")))))
      )))

    def post(
              edit: Boolean = false,
              valid: Boolean = true,
              activities: BusinessActivities = BusinessActivities(Set.empty)
            )(block: Result => Unit) = block({

      when(cache.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(
          activities = Some(activities)
        )))

      await(controller.post(edit)(formRequest(valid)))
    })
  }

  "The customer outside uk controller" when {

    "get is called" must {
      "load the page" in new Fixture {

        when(renewalService.getRenewal(any(),any(),any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)

        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        val pageTitle = Messages("renewal.customer.outside.uk.title") + " - " +
          Messages("summary.renewal") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        document.title() mustBe pageTitle
      }

      "pre-populate the Customer outside UK Page" in new Fixture {

        when(renewalService.getRenewal(any(),any(),any()))
          .thenReturn(Future.successful(Some(Renewal(customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB")))))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isOutside]").size mustEqual 2
        document.select("input[name=isOutside][checked]").`val` mustEqual "true"
        document.select("select[name=countries[0]] > option[value=GB]").hasAttr("selected") must be(true)

      }

    }

    "post is called" when {

      "given valid data" must {

        "redirect to the summary page" when {
          "business is not an hvd" in new FormSubmissionFixture {
            post() { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(routes.SummaryController.get().url)
            }
          }
        }

        "redirect to the PercentageOfCashPaymentOver15000Controller" when {
          "business is an hvd" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "isOutside" -> "true",
              "countries[0]" -> "GB",
              "countries[1]" -> "US"
            )

            when(dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(activities = Some(BusinessActivities(Set(HighValueDealing))))))

            when(mockCacheMap.getEntry[Renewal](Renewal.key))
              .thenReturn(Some(Renewal()))

            when(dataCacheConnector.save[Renewal](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PercentageOfCashPaymentOver15000Controller.get().url))
          }
        }

        "redirect to the Msb Turnover page" when {
          "business is an msb" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "isOutside" -> "true",
              "countries[0]" -> "GB",
              "countries[1]" -> "US"
            )

            when(dataCacheConnector.fetchAll(any(), any()))
              .thenReturn(Future.successful(Some(mockCacheMap)))

            when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
              .thenReturn(Some(BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness, HighValueDealing))))))

            when(mockCacheMap.getEntry[Renewal](Renewal.key))
              .thenReturn(Some(Renewal()))

            when(dataCacheConnector.save[Renewal](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.TotalThroughputController.get().url))
          }
        }

      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new FormSubmissionFixture {
          post(valid = false) { result =>
            result.header.status mustBe BAD_REQUEST
          }
        }
      }
    }

  }

  it must {
    "remove data from SendTheLargestAmountsOfMoney and MostTransactions" when {
      "CustomersOutsideUK is edited from yes to no" in new FormSubmissionFixture {

        post(edit = true) { result =>
          result.header.status mustBe SEE_OTHER

          verify(renewalService)
            .updateRenewal(Renewal(
              customersOutsideUK = Some(CustomersOutsideUK(None)),
              sendTheLargestAmountsOfMoney = None,
              mostTransactions = None
            ))(any(), any(), any())
        }

      }
    }
  }
}
