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

package controllers.businessmatching

import cats.data.OptionT
import cats.implicits._
import connectors.DataCacheConnector
import forms.{EmptyForm, Form2}
import generators.ResponsiblePersonGenerator
import models.businessmatching._
import models.responsiblepeople.ResponsiblePeople
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.PrivateMethodTester
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}
import org.scalacheck.Gen

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RegisterServicesControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with PrivateMethodTester with ResponsiblePersonGenerator {

  val activities: Set[BusinessActivity] = Set(
    AccountancyServices,
    BillPaymentServices,
    EstateAgentBusinessService,
    HighValueDealing,
    MoneyServiceBusiness,
    TrustAndCompanyServices,
    TelephonePaymentService
  )

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val statusService = mockStatusService
    val businessMatchingService = mock[BusinessMatchingService]

    val activityData1: Set[BusinessActivity] = Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)
    val activityData2: Set[BusinessActivity] = Set(HighValueDealing, MoneyServiceBusiness)

    val businessActivities1 = BusinessActivities(activityData1)

    val businessMatching1 = BusinessMatching(None, Some(businessActivities1))

    lazy val app = new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[BusinessMatchingService].to(businessMatchingService))
      .overrides(bind[StatusService].to(statusService))
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .build()

    val controller = app.injector.instanceOf[RegisterServicesController]

    when {
      controller.statusService.isPreSubmission(any(), any(), any())
    } thenReturn Future.successful(true)

    when {
      controller.businessMatchingService.commitVariationData(any(),any(),any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)

    when {
      controller.businessMatchingService.updateModel(any())(any(),any(),any())
    } thenReturn OptionT.some[Future, CacheMap](mockCacheMap)

    def anyBoolean = Gen.oneOf[Boolean](true, false).sample.get

    val responsiblePerson = responsiblePersonGen.sample.get
    val responsiblePersonChanged = responsiblePerson.copy(hasChanged = true, hasAccepted = true)

    val fitAndProperResponsiblePeople = Seq(
      responsiblePerson.copy(hasAlreadyPassedFitAndProper = Some(true)),
      responsiblePerson.copy(hasAlreadyPassedFitAndProper = Some(false))
    )

    mockCacheFetch[Seq[ResponsiblePeople]](Some(fitAndProperResponsiblePeople), Some(ResponsiblePeople.key))
    mockCacheSave[Seq[ResponsiblePeople]]

  }

  "RegisterServicesController" when {

    "get is called" must {

      "display the view" which {
        "shows empty fields" in new Fixture {

          when(controller.businessMatchingService.getModel(any(),any(),any()))
            .thenReturn(OptionT.some[Future, BusinessMatching](BusinessMatching()))

          val result = controller.get()(request)
          status(result) must be(OK)
          contentAsString(result) must include("   ")
        }

        "populates fields" in new Fixture {

          when {
            controller.businessMatchingService.getModel(any(), any(), any())
          } thenReturn OptionT.some[Future, BusinessMatching](businessMatching1)

          val result = controller.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          private val checkbox = document.select("input[id=businessActivities-01]")
          checkbox.attr("checked") must be("checked")
        }
      }
    }

    "post is called" when {
      "request data is valid" must {
        "redirect to SummaryController" when {
          "edit is false" in new Fixture {

            val businessActivitiesWithData = BusinessActivities(businessActivities = activityData1)
            val businessMatchingWithData = BusinessMatching(None, Some(businessActivitiesWithData))

            val newRequest = request.withFormUrlEncodedBody(
              "businessActivities" -> "01",
              "businessActivities" -> "02",
              "businessActivities" -> "03")

            when(controller.businessMatchingService.getModel(any(), any(), any()))
              .thenReturn(OptionT.some[Future, BusinessMatching](businessMatchingWithData))

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }

          "edit is true" in new Fixture {

            val businessActivitiesWithData = BusinessActivities(businessActivities = activityData2)
            val businessMatchingWithData = BusinessMatching(None, Some(businessActivitiesWithData))

            val newRequest = request.withFormUrlEncodedBody(
              "businessActivities" -> "01",
              "businessActivities" -> "02",
              "businessActivities" -> "03")

            when(controller.businessMatchingService.getModel(any(), any(), any()))
              .thenReturn(OptionT.some[Future, BusinessMatching](businessMatchingWithData))

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }
        }

        "redirect to ServicesController" when {
          "msb is selected" in new Fixture {

            val businessActivities = BusinessActivities(businessActivities = Set(HighValueDealing, MoneyServiceBusiness))
            val bm = BusinessMatching(None, Some(businessActivities))

            val newRequest = request.withFormUrlEncodedBody(
              "businessActivities[0]" -> "04",
              "businessActivities[1]" -> "05")

            when(controller.businessMatchingService.getModel(any(), any(), any()))
              .thenReturn(OptionT.some[Future, BusinessMatching](bm))

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.ServicesController.get(false).url))
          }
        }
      }

      "request data is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val businessActivitiesWithData = BusinessActivities(businessActivities = activityData1)
          val businessMatchingWithData = BusinessMatching(None, Some(businessActivitiesWithData))

          val newRequest = request.withFormUrlEncodedBody(
            "businessActivities" -> "11")

          when(controller.businessMatchingService.getModel(any(), any(), any()))
            .thenReturn(OptionT.some[Future, BusinessMatching](businessMatchingWithData))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }
    }

    "getActivityValues is called" must {
      "return values for all services" when {
        "status is pre-submission" when {
          "activities have not yet been selected" in new Fixture {

            val getActivityValues = PrivateMethod[(Set[String], Set[String])]('getActivityValues)

            val (newActivities, existing) = controller invokePrivate getActivityValues(EmptyForm, true, None)

            activities foreach { act =>
              newActivities must contain(BusinessActivities.getValue(act))
            }

            existing must be(empty)

          }
          "activities have already been selected" in new Fixture {

            val getActivityValues = PrivateMethod[(Set[String], Set[String])]('getActivityValues)

            val (newActivities, existing) =
              controller invokePrivate getActivityValues(Form2[BusinessActivities](businessActivities1), true, Some(activityData1))

            activities foreach { act =>
              newActivities must contain(BusinessActivities.getValue(act))
            }

            existing must be(empty)

          }
        }
      }
      "return values for services excluding those provided" when {
        "status is post-submission" when {

          activities.foreach { act =>

            s"$act is contained in existing activities" in new Fixture {

              val activityData: Set[BusinessActivity] = Set(act)
              val businessActivities = businessActivities1.copy(businessActivities = activityData, removeActivities = None, dateOfChange = None)

              val getActivityValues = PrivateMethod[(Set[String], Set[String])]('getActivityValues)

              val (newActivities, existing) =
                controller invokePrivate getActivityValues(Form2[BusinessActivities](businessActivities), false, Some(activityData))

              newActivities must not contain BusinessActivities.getValue(act)
              existing must be(Set(BusinessActivities.getValue(act)))

            }

          }

        }
      }
    }

    "updateModel" must {
      "add data to the existing services" when {
        "status is post-submission" in new Fixture {

          val existingServices = BusinessActivities(Set(HighValueDealing, AccountancyServices))
          val addedServices = BusinessActivities(Set(MoneyServiceBusiness, TelephonePaymentService))

          val updateModel = PrivateMethod[BusinessActivities]('updateModel)

          val services = controller invokePrivate updateModel(Some(existingServices), addedServices, false)

          services must be(BusinessActivities(existingServices.businessActivities, Some(addedServices.businessActivities)))

        }
      }
      "replace existing services" when {
        "status is pre-submission" in new Fixture {

          val existingServices = BusinessActivities(Set(HighValueDealing, AccountancyServices))
          val addedServices = BusinessActivities(Set(MoneyServiceBusiness, TelephonePaymentService))

          val updateModel = PrivateMethod[BusinessActivities]('updateModel)

          val services = controller invokePrivate updateModel(Some(existingServices), addedServices, true)

          services must be(addedServices)

        }
      }
    }
  }

  it must {
    "save additional services as additionalActivities" when {
      "status is post-submission" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any(),any(),any())
        } thenReturn OptionT.some[Future, BusinessMatching](businessMatching1)

        when {
          controller.statusService.isPreSubmission(any(),any(),any())
        } thenReturn Future.successful(false)

        val result = controller.post()(request.withFormUrlEncodedBody(
          "businessActivities[0]" -> BusinessActivities.getValue(HighValueDealing),
          "businessActivities[1]" -> BusinessActivities.getValue(TelephonePaymentService)
        ))

        status(result) must be(SEE_OTHER)

        verify(controller.businessMatchingService).updateModel(eqTo(businessMatching1.activities(
            BusinessActivities(activityData1, Some(Set(HighValueDealing, TelephonePaymentService)))
        )))(any(),any(),any())

      }
    }
    "save only services from request to businessActivties" when {
      "status is pre-submisson" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any(),any(),any())
        } thenReturn OptionT.some[Future, BusinessMatching](businessMatching1)

        when {
          controller.statusService.isPreSubmission(any(),any(),any())
        } thenReturn Future.successful(true)

        val result = controller.post()(request.withFormUrlEncodedBody(
          "businessActivities[0]" -> BusinessActivities.getValue(HighValueDealing),
          "businessActivities[1]" -> BusinessActivities.getValue(TelephonePaymentService)
        ))

        status(result) must be(SEE_OTHER)

        verify(controller.businessMatchingService).updateModel(eqTo(businessMatching1.activities(
          BusinessActivities(Set(HighValueDealing, TelephonePaymentService))
        )))(any(),any(),any())

      }
    }
    "remove RP FitAndProper" when {
      "tcsp is removed and msb is not selected" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any(),any(),any())
        } thenReturn OptionT.some[Future, BusinessMatching](BusinessMatching(None, Some(BusinessActivities(Set(TrustAndCompanyServices, HighValueDealing)))))

        when {
          controller.statusService.isPreSubmission(any(),any(),any())
        } thenReturn Future.successful(anyBoolean)

        val result = controller.post()(request.withFormUrlEncodedBody(
          "businessActivities[0]" -> BusinessActivities.getValue(HighValueDealing)
        ))

        status(result) must be(SEE_OTHER)

        verify(mockCacheConnector).save[Seq[ResponsiblePeople]](
          eqTo(ResponsiblePeople.key),
          eqTo(Seq(responsiblePersonChanged, responsiblePersonChanged))
        )(any(),any(),any())

      }
      "msb is removed tcsp is not selected" in new Fixture {

      }
    }
    "not update RP" when {
      "tcsp is removed and msb exists in business activities" in new Fixture {

      }
      "msb is removed and tcsp exists in business activities" in new Fixture {

      }
      "tcsp is removed and msb is selected in request" in new Fixture {

      }
      "msb is removed and tcsp is selected in request" in new Fixture {

      }

    }
  }

}