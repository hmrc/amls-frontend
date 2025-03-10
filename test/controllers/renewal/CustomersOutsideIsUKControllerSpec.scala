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

import controllers.actions.SuccessfulAuthAction
import models.Country
import models.businessmatching.BusinessActivity._
import models.businessmatching._
import models.renewal._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.RenewalService
import services.cache.Cache
import utils.{AmlsSpec, AuthAction, AuthorisedFixture}

import scala.concurrent.Future

class CustomersOutsideIsUKControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val renewalService = mock[RenewalService]
    val authAction     = SuccessfulAuthAction

    val emptyCache   = Cache.empty
    val mockCacheMap = mock[Cache]

    lazy val app = new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .overrides(bind[RenewalService].to(renewalService))
      .overrides(bind[AuthAction].to(authAction))
      .build()

    val controller = app.injector.instanceOf[CustomersOutsideIsUKController]

  }

  trait FormSubmissionFixture extends Fixture {

    def formData(data: Option[Request[AnyContentAsFormUrlEncoded]]) = data match {
      case Some(d) => d
      case None    =>
        FakeRequest(POST, routes.CustomersOutsideUKController.post().url)
          .withFormUrlEncodedBody("isOutside" -> "true")
    }

    def formRequest(data: Option[Request[AnyContentAsFormUrlEncoded]]) = formData(data)

    val cache = mock[Cache]

    val sendTheLargestAmountsOfMoney = SendTheLargestAmountsOfMoney(Seq(Country("GB", "GB")))
    val mostTransactions             = MostTransactions(Seq(Country("GB", "GB")))
    val customersOutsideUK           = CustomersOutsideUK(Some(Seq(Country("GB", "GB"))))
    val customersOutsideIsUK         = CustomersOutsideIsUK(true)

    when {
      renewalService.fetchAndUpdateRenewal(any(), any())
    } thenReturn Future.successful(Some(cache))

    when {
      cache.getEntry[Renewal](Renewal.key)
    } thenReturn Some(
      Renewal(
        customersOutsideUK = Some(customersOutsideUK),
        sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
        mostTransactions = Some(mostTransactions)
      )
    )

    def post(
      edit: Boolean = false,
      data: Option[Request[AnyContentAsFormUrlEncoded]] = None,
      businessMatching: BusinessMatching = BusinessMatching(activities = Some(BusinessActivities(Set.empty))),
      renewal: Option[Renewal] = None
    )(block: Result => Unit) = block {

      when {
        cache.getEntry[Renewal](Renewal.key)
      } thenReturn Some(renewal match {
        case Some(r) => r
        case None    =>
          Renewal(
            customersOutsideIsUK = Some(customersOutsideIsUK),
            customersOutsideUK = Some(customersOutsideUK),
            sendTheLargestAmountsOfMoney = Some(sendTheLargestAmountsOfMoney),
            mostTransactions = Some(mostTransactions)
          )
      })

      when {
        renewalService.getBusinessMatching(any())
      } thenReturn Future.successful(Some(businessMatching))

      await(controller.post(edit)(formRequest(data)))
    }
  }

  "The customer outside uk controller" when {

    "get is called" must {
      "load the page" in new Fixture {

        when(renewalService.getRenewal(any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)

        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        val pageTitle = messages("renewal.customer.outside.uk.title") + " - " +
          messages("summary.renewal") + " - " +
          messages("title.amls") + " - " + messages("title.gov")

        document.title() mustBe pageTitle
      }

      "pre-populate the Customer outside UK Page" in new Fixture {

        when(renewalService.getRenewal(any()))
          .thenReturn(Future.successful(Some(Renewal(customersOutsideIsUK = Some(CustomersOutsideIsUK(true))))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isOutside]").size mustEqual 2
        document.select("input[name=isOutside][checked]").`val` mustEqual "true"

      }

    }

    "post is called" when {

      "updating the cache with user answer fails" must {

        "return 500" in new FormSubmissionFixture {
          when {
            renewalService.fetchAndUpdateRenewal(any(), any())
          } thenReturn Future.successful(None)
          post() { res =>
            status(Future.successful(res)) mustBe INTERNAL_SERVER_ERROR
          }
        }
      }

      "given valid data" must {

        "redirect to the CustomersOutsideUK page" when {

          "in edit mode" in new FormSubmissionFixture {
            post(
              edit = true,
              businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness))))
            ) { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(routes.CustomersOutsideUKController.get(true).url)
            }
          }

          "business is an asp and not an hvd or an msb" in new FormSubmissionFixture {
            post(businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(AccountancyServices))))) {
              result =>
                result.header.status mustBe SEE_OTHER
                result.header.headers.get("Location") mustBe Some(routes.CustomersOutsideUKController.get().url)
            }
          }
        }

        "redirect to the PercentageOfCashPaymentsOver15000 page" when {

          "user answers no, not in edit mode and business is an hvd" in new FormSubmissionFixture {
            post(
              businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(HighValueDealing)))),
              data = Some(
                addToken(
                  FakeRequest(POST, routes.CustomersOutsideIsUKController.post().url)
                    .withFormUrlEncodedBody("isOutside" -> "false")
                )
              )
            ) { result =>
              result.header.status mustBe SEE_OTHER
              result.header.headers.get("Location") mustBe Some(
                routes.PercentageOfCashPaymentOver15000Controller.get().url
              )
            }
          }
        }

        "business is an msb and asp" in new FormSubmissionFixture {
          post(businessMatching =
            BusinessMatching(activities = Some(BusinessActivities(Set(MoneyServiceBusiness, AccountancyServices))))
          ) { result =>
            result.header.status mustBe SEE_OTHER
            result.header.headers.get("Location") mustBe Some(routes.CustomersOutsideUKController.get().url)
          }
        }
      }

    }

    "given invalid data" must {
      "respond with BAD_REQUEST" in new FormSubmissionFixture {
        post(data = Some(addToken(authRequest.withFormUrlEncodedBody("isOutside" -> "abc")))) { result =>
          result.header.status mustBe BAD_REQUEST
        }
      }
    }
  }

}
