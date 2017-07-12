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
import models.renewal.{CustomersOutsideUK, MostTransactions, Renewal, SendTheLargestAmountsOfMoney}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
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

    def formData(data: Option[FakeRequest[AnyContentAsFormUrlEncoded]]) = data match {
      case Some(d) => d
      case None => request.withFormUrlEncodedBody("isOutside" -> "false")
    }

    def formRequest(data: Option[FakeRequest[AnyContentAsFormUrlEncoded]]) = formData(data)

    val cache = mock[CacheMap]

    val sendTheLargestAmountsOfMoney = SendTheLargestAmountsOfMoney(Country("GB","GB"))
    val mostTransactions = MostTransactions(Seq(Country("GB","GB")))
    val customersOutsideUK = CustomersOutsideUK(Some(Seq(Country("GB", "GB"))))

    when {
      renewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(cache)

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))

    when {
      cache.getEntry[Renewal](Renewal.key)
    } thenReturn Some(Renewal(
      customersOutsideUK = Some(customersOutsideUK),
      sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
      mostTransactions = Some(mostTransactions)
    ))

    def post(
              edit: Boolean = false,
              data: Option[FakeRequest[AnyContentAsFormUrlEncoded]] = None,
              activities: BusinessActivities = BusinessActivities(Set.empty),
              renewal: Option[Renewal] = None
            )(block: Result => Unit) = block({

      when {
        cache.getEntry[Renewal](Renewal.key)
      } thenReturn Some(renewal match {
        case Some(r) => r
        case None => Renewal(
          customersOutsideUK = Some(customersOutsideUK),
          sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
          mostTransactions = Some(mostTransactions)
        )
      })

      when {
        cache.getEntry[BusinessMatching](BusinessMatching.key)
      } thenReturn{
        Some(BusinessMatching(activities = Some(activities)))
      }

      await(controller.post(edit)(formRequest(data)))
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
          "business is an hvd" in new FormSubmissionFixture {
            post(activities = BusinessActivities(Set(HighValueDealing))) { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(routes.PercentageOfCashPaymentOver15000Controller.get().url)
            }
          }
        }

        "redirect to the Msb Turnover page" when {
          "business is an msb" in new FormSubmissionFixture {
            post(activities = BusinessActivities(Set(MoneyServiceBusiness))) { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(routes.TotalThroughputController.get().url)
            }
          }
        }

        "redirect to SendTheLargestAmountsOfMoneyController" when {
          "edit is true" when {
            "business is an msb including Transmitting Money services" when {
              "CustomersOutsideUK is edited from no to yes" in new FormSubmissionFixture {

                val data = request.withFormUrlEncodedBody(
                  "isOutside" -> "true",
                  "countries[0]" -> "US")

                val renewal = Renewal(
                  customersOutsideUK = Some(CustomersOutsideUK(None))
                )

                post(
                  edit = true,
                  data = Some(data),
                  activities = BusinessActivities(Set(MoneyServiceBusiness)),
                  renewal = Some(renewal)
                ) { result =>
                  result.header.status mustBe SEE_OTHER
                  result.header.headers.get("Location") mustBe Some(routes.SendTheLargestAmountsOfMoneyController.get().url)
                }
              }
            }
          }
        }

      }

      "respond with BAD_REQUEST" when {
        "given invalid data" in new FormSubmissionFixture {
          post(data = Some(request.withFormUrlEncodedBody("isOutside" -> "abc"))) { result =>
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
            .updateRenewal(eqTo(Renewal(
              customersOutsideUK = Some(CustomersOutsideUK(None)),
              sendTheLargestAmountsOfMoney = None,
              mostTransactions = None,
              hasChanged = true
            )))(any(), any(), any())
        }

      }
    }
    "keep data from SendTheLargestAmountsOfMoney and MostTransactions" when {
      "only countries are changed in the update of renewal" in new FormSubmissionFixture {

        val data = request.withFormUrlEncodedBody(
          "isOutside" -> "true",
          "countries[0]" -> "US")

        post(edit = true, data = Some(data)) { result =>
          result.header.status mustBe SEE_OTHER

          verify(renewalService)
            .updateRenewal(eqTo(Renewal(
              customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("United States","US"))))),
              sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
              mostTransactions = Some(mostTransactions),
              hasChanged = true
            )))(any(), any(), any())
        }

      }
    }
  }
}
