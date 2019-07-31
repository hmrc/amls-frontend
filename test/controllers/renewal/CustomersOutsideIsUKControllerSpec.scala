/*
 * Copyright 2019 HM Revenue & Customs
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
import models.businessmatching._
import models.renewal._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class CustomersOutsideIsUKControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

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

    val controller = app.injector.instanceOf[CustomersOutsideIsUKController]

  }

  trait FormSubmissionFixture extends Fixture {

    def formData(data: Option[FakeRequest[AnyContentAsFormUrlEncoded]]) = data match {
      case Some(d) => d
      case None => request.withFormUrlEncodedBody("isOutside" -> "true")
    }

    def formRequest(data: Option[FakeRequest[AnyContentAsFormUrlEncoded]]) = formData(data)

    val cache = mock[CacheMap]

    val sendTheLargestAmountsOfMoney = SendTheLargestAmountsOfMoney(Seq(Country("GB", "GB")))
    val mostTransactions = MostTransactions(Seq(Country("GB", "GB")))
    val customersOutsideUK = CustomersOutsideUK(Some(Seq(Country("GB", "GB"))))
    val customersOutsideIsUK = CustomersOutsideIsUK(true)

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
              businessMatching: BusinessMatching = BusinessMatching(activities = Some(BusinessActivities(Set.empty))),
              renewal: Option[Renewal] = None
            )(block: Result => Unit) = block({

      when {
        cache.getEntry[Renewal](Renewal.key)
      } thenReturn Some(renewal match {
        case Some(r) => r
        case None => Renewal(
          customersOutsideIsUK = Some(customersOutsideIsUK),
          customersOutsideUK = Some(customersOutsideUK),
          sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
          mostTransactions = Some(mostTransactions)
        )
      })

      when {
        cache.getEntry[BusinessMatching](BusinessMatching.key)
      } thenReturn {
        Some(businessMatching)
      }

      await(controller.post(edit)(formRequest(data)))
    })
  }

  "The customer outside uk controller" when {

    "get is called" must {
      "load the page" in new Fixture {

        when(renewalService.getRenewal(any(), any(), any()))
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

        when(renewalService.getRenewal(any(), any(), any()))
          .thenReturn(Future.successful(Some(Renewal(customersOutsideIsUK = Some(CustomersOutsideIsUK(true))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isOutside]").size mustEqual 2
        document.select("input[name=isOutside][checked]").`val` mustEqual "true"

      }

    }

    "post is called" when {

      "given valid data" must {

        "redirect to the CustomersOutsideUK page" when {

          "in edit mode" in new FormSubmissionFixture {
            post(edit = true, businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness))))) { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(routes.CustomersOutsideUKController.get(true).url)
            }
          }

          "business is an asp and not an hvd or an msb" in new FormSubmissionFixture {
            post(businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(AccountancyServices))))) { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(routes.CustomersOutsideUKController.get().url)
            }
          }
        }

        "redirect to the PercentageOfCashPaymentsOver15000 page" when {

          "user answers no, not in edit mode and business is an hvd" in new FormSubmissionFixture {
            post(
              businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(HighValueDealing)))),
              data = Some(request.withFormUrlEncodedBody("isOutside" -> "false"))
            ) { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(routes.PercentageOfCashPaymentOver15000Controller.get().url)
            }
          }
        }


        "business is an msb and asp" in new FormSubmissionFixture {
          post(businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness, AccountancyServices))))) { result =>
            result.header.status mustBe SEE_OTHER
            result.header.headers.get("Location") mustBe Some(routes.CustomersOutsideUKController.get().url)
          }
        }
      }

    }

    "given invalid data" must {
      "respond with BAD_REQUEST" in new FormSubmissionFixture {
        post(data = Some(request.withFormUrlEncodedBody("isOutside" -> "abc"))) { result =>
          result.header.status mustBe BAD_REQUEST
        }
      }
    }
  }

}
